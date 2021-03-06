package ubc.pavlab.rdp.services;

import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.PasswordResetTokenRepository;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.repositories.UserRepository;
import ubc.pavlab.rdp.repositories.VerificationTokenRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import javax.validation.ValidationException;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

/**
 * Created by mjacobson on 16/01/18.
 */
@Service("userService")
@CommonsLog
public class UserServiceImpl implements UserService {

    @Autowired
    ApplicationSettings applicationSettings;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired
    private VerificationTokenRepository tokenRepository;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private GOService goService;

    @Autowired
    PrivacyService privacyService;

    private Role roleAdmin;

    @SuppressWarnings("unused") // Keeping for future use
    private static <T> Collector<T, ?, List<T>> maxList( Comparator<? super T> comp ) {
        return Collector.of( ArrayList::new, ( list, t ) -> {
            int c;
            if ( list.isEmpty() || ( c = comp.compare( t, list.get( 0 ) ) ) == 0 ) {
                list.add( t );
            } else if ( c > 0 ) {
                list.clear();
                list.add( t );
            }
        }, ( list1, list2 ) -> {
            if ( list1.isEmpty() ) {
                return list2;
            }
            if ( list2.isEmpty() ) {
                return list1;
            }
            int r = comp.compare( list1.get( 0 ), list2.get( 0 ) );
            if ( r < 0 ) {
                return list2;
            } else if ( r > 0 ) {
                return list1;
            } else {
                list1.addAll( list2 );
                return list1;
            }
        } );
    }

    private static <T> Collector<T, ?, Set<T>> maxSet( Comparator<? super T> comp ) {
        return Collector.of( HashSet::new, ( set, t ) -> {
            int c;
            if ( set.isEmpty() || ( c = comp.compare( t, set.iterator().next() ) ) == 0 ) {
                set.add( t );
            } else if ( c > 0 ) {
                set.clear();
                set.add( t );
            }
        }, ( set1, set2 ) -> {
            if ( set1.isEmpty() ) {
                return set2;
            }
            if ( set2.isEmpty() ) {
                return set1;
            }
            int r = comp.compare( set1.iterator().next(), set2.iterator().next() );
            if ( r < 0 ) {
                return set2;
            } else if ( r > 0 ) {
                return set1;
            } else {
                set1.addAll( set2 );
                return set1;
            }
        } );
    }

    @Transactional
    @Override
    public User create( User user ) {
        user.setPassword( bCryptPasswordEncoder.encode( user.getPassword() ) );
        Role userRole = roleRepository.findByRole( "ROLE_USER" );

        user.setRoles( Collections.singleton( userRole ) );
        return userRepository.save( user );
    }

    @Transactional
    @Override
    public User update( User user ) {
        // Currently restrict to updating your own user. Can make this better with
        // Pre-Post authorized annotations.
        String currentUsername = getCurrentEmail();
        if ( user.getEmail().equals( currentUsername ) ) {
	    if (applicationSettings.getPrivacy() == null){
		log.warn( currentUsername + " attempted to updated, but applicationSettings is null. ");
	    } else {
                PrivacyLevelType defaultPrivacyLevel = PrivacyLevelType.values()[applicationSettings.getPrivacy().getDefaultLevel()];
		boolean defaultSharing = applicationSettings.getPrivacy().isDefaultSharing();
		boolean defaultGenelist = applicationSettings.getPrivacy().isAllowHideGenelist();
		
		if (user.getProfile().getPrivacyLevel() == null){		
		    log.warn("Received a null 'privacyLevel' value in profile.");
		    user.getProfile().setPrivacyLevel(defaultPrivacyLevel);
		}
		if (user.getProfile().getShared() == null){	
		    log.warn("Received a null 'shared' value in profile.");
		    user.getProfile().setShared(defaultSharing);
		}
		if (user.getProfile().getHideGenelist() == null){
		    log.warn("Received a null 'hideGeneList' value in profile.");
		    user.getProfile().setHideGenelist(defaultGenelist);
		}	
	    }

            PrivacyLevelType userPrivacyLevel = user.getProfile().getPrivacyLevel();

            // We cap the user gene privacy level to its new profile setting
            for ( UserGene gene : user.getUserGenes().values() ) {
                PrivacyLevelType genePrivacyLevel = gene.getPrivacyLevel();
                // in case any of the user or gene privacy level is null, we already have a cascading value
                if ( userPrivacyLevel == null || genePrivacyLevel == null ) {
                    continue;
                }
                if ( userPrivacyLevel.ordinal() < genePrivacyLevel.ordinal() ) {
                    gene.setPrivacyLevel( userPrivacyLevel );
                    log.info( "Privacy level of " + gene + " will be capped to " + userPrivacyLevel + " (was " + genePrivacyLevel + ")." );
                }
            }

            return userRepository.save( user );
	    
        } else {
            log.warn( currentUsername + " attempted to update a user that is not their own: " + user.getEmail() );
        }

        return null;

    }

    @Transactional
    @Override
    public void delete( User user ) {

        VerificationToken verificationToken = tokenRepository.findByUser( user );

        if ( verificationToken != null ) {
            tokenRepository.delete( verificationToken );
        }

        PasswordResetToken passwordToken = passwordResetTokenRepository.findByUser( user );

        if ( passwordToken != null ) {
            passwordResetTokenRepository.delete( passwordToken );
        }

        userRepository.delete( user );
    }

    @Override
    public User changePassword( String oldPassword, String newPassword )
            throws BadCredentialsException, ValidationException {
        User user = findCurrentUser();
        if ( bCryptPasswordEncoder.matches( oldPassword, user.getPassword() ) ) {
            if ( newPassword.length() >= 6 ) { //TODO: Tie in with hibernate constraint on User or not necessary?

                user.setPassword( bCryptPasswordEncoder.encode( newPassword ) );
                return update( user );
            } else {
                throw new ValidationException( "Password must be a minimum of 6 characters" );
            }
        } else {
            throw new BadCredentialsException( "Password incorrect" );
        }
    }

    @Override
    public String getCurrentUserName() {
        return getCurrentEmail();
    }

    @Override
    public String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    @Override
    public User findCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();	
        if ( auth == null || auth.getPrincipal().equals( "anonymousUser" ) ) {
            return null;
        }
	
        return findUserByIdNoAuth( ( ( UserPrinciple ) auth.getPrincipal() ).getId() );
    }

    @Override
    public User findUserById( int id ) {
        User user = userRepository.findOne( id );
        return user == null ? null : privacyService.checkCurrentUserCanSee( user ) ? user : null;
    }

    @Override
    public User findUserByIdNoAuth( int id ) {
        // Only use this in placed where no authentication of user is needed
        return userRepository.findOne( id );
    }

    @Override
    public User findUserByEmail( String email ) {
        return userRepository.findByEmailIgnoreCase( email );
    }

    @Override
    public User getRemoteAdmin() {
        return userRepository.findOne( applicationSettings.getIsearch().getUserId() );
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll().stream()
                .filter(privacyService::checkCurrentUserCanSee)
                .collect( Collectors.toList() );
    }

    @Override
    public Collection<User> findByLikeName( String nameLike ) {
        return userRepository.findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( nameLike, nameLike )
                .stream()
                .filter(privacyService::checkCurrentUserCanSee)
                .collect( Collectors.toList() );
    }

    @Override
    public Collection<User> findByStartsName( String startsName ) {
        return userRepository
                .findByProfileLastNameStartsWithIgnoreCase( startsName )
                .stream()
                .filter(privacyService::checkCurrentUserCanSee)
                .collect( Collectors.toList() );
    }

    @Override
    public Collection<User> findByDescription( String descriptionLike ) {
        return userRepository
                .findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase( descriptionLike, descriptionLike )
                .stream()
                .filter(privacyService::checkCurrentUserCanSee)
                .collect( Collectors.toList() );
    }

    @Override
    public long countResearchers() {
        return userRepository.count();
    }

    @Override
    public Collection<UserTerm> convertTerms( User user, Taxon taxon, Collection<GeneOntologyTerm> terms ) {
        Set<UserGene> genes = user.getGenesByTaxonAndTier( taxon, TierType.MANUAL_TIERS );

        return convertTermTypes( terms, taxon, genes );
    }

    @Override
    public UserTerm convertTerms( User user, Taxon taxon, GeneOntologyTerm term ) {
        Set<UserGene> genes = user.getGenesByTaxonAndTier( taxon, TierType.MANUAL_TIERS );

        return convertTermTypes( term, taxon, genes );
    }

    @Override
    public Collection<UserTerm> recommendTerms( User user, Taxon taxon ) {
        return recommendTerms( user, taxon, 10, applicationSettings.getGoTermSizeLimit(), 2 );
    }

    @Override
    public Collection<UserTerm> recommendTerms( User user, Taxon taxon, int minSize, int maxSize, int minFrequency ) {
        if ( user == null || taxon == null )
            return null;

        Set<UserGene> genes = user.getGenesByTaxonAndTier( taxon, TierType.MANUAL_TIERS );

        Map<GeneOntologyTerm, Long> fmap = goService.termFrequencyMap( genes );

        Stream<Map.Entry<GeneOntologyTerm, Long>> resultStream = fmap.entrySet().stream();

        // Filter out terms without enough overlap or that are too broad/specific
        if ( minFrequency > 0 ) {
            resultStream = resultStream.filter( e -> e.getValue() >= minFrequency );
        }

        if ( minSize > 0 ) {
            resultStream = resultStream.filter( e -> e.getKey().getSize( taxon ) >= minSize );
        }

        if ( maxSize > 0 ) {
            resultStream = resultStream.filter( e -> e.getKey().getSize( taxon ) <= maxSize );
        }

        Set<UserTerm> userTerms = user.getTermsByTaxon( taxon );

        // Then keep only those terms not already added and with the highest frequency
        Set<UserTerm> topResults = resultStream.map( e -> {
            UserTerm ut = new UserTerm( e.getKey(), taxon, null );
            ut.setFrequency( e.getValue().intValue() );
            return ut;
        } ).filter( ut -> !userTerms.contains( ut ) ).collect( maxSet( comparing( UserTerm::getFrequency ) ) );

        // Keep only leafiest of remaining terms (keep if it has no descendants in results)
        return topResults.stream().filter( ut -> Collections.disjoint( topResults, goService.getDescendants( ut ) ) )
                .collect( Collectors.toSet() );
    }

    @Transactional
    @Override
    public void updateTermsAndGenesInTaxon( User user, Taxon taxon, Map<Gene, TierType> genesToTierMap,
                                            Map<Gene, Optional<PrivacyLevelType>> genesToPrivacyLevelMap,
            Collection<GeneOntologyTerm> goTerms ) {
        // Remove genes from other taxons (they shouldn't be here but just incase)
        genesToTierMap.keySet().removeIf( e -> !e.getTaxon().equals( taxon ) );
        int initialSize = user.getUserGenes().size();

        // Update terms

        // Inform Hibernate of similar entities
        Map<String, Integer> goIdToHibernateId = user.getUserTerms().stream()
                .filter( t -> t.getTaxon().equals( taxon ) )
                .collect( Collectors.toMap( GeneOntologyTerm::getGoId, UserTerm::getId ) );
        Collection<UserTerm> updatedTerms = convertTermTypes( goTerms, taxon, genesToTierMap.keySet() );
        updatedTerms.forEach( t -> t.setId( goIdToHibernateId.get( t.getGoId() ) ) );

        removeTermsFromUserByTaxon( user, taxon );
        user.getUserTerms().addAll( updatedTerms );

        for ( Gene gene : calculatedGenesInTaxon( user, taxon ) ) {
            genesToTierMap.putIfAbsent( gene, TierType.TIER3 );
        }

        int updated = 0;
        for ( Map.Entry<Gene, TierType> entry : genesToTierMap.entrySet() ) {
            Optional<PrivacyLevelType> privacyLevel = genesToPrivacyLevelMap.get( entry.getKey() );
            updated += updateOrInsert( user, entry.getKey(), entry.getValue(), privacyLevel ) ? 1 : 0;
        }

        int added = user.getUserGenes().size() - initialSize;

        // Remove genes that no longer belong in this taxon
        user.getUserGenes().values().removeIf( g -> g.getTaxon().equals( taxon ) && !genesToTierMap.containsKey( g ) );

        int removed = user.getUserGenes().size() - ( initialSize + added );

        log.info( "Added: " + added + ", removed: " + removed + ", updated: " + updated + " genes, User " + user
                .getEmail() );

        update( user );
    }

    @Transactional
    @Override
    public User updatePublications( User user, Set<Publication> publications ) {
        user.getProfile().getPublications().retainAll( publications );
        user.getProfile().getPublications().addAll( publications );
        return update( user );
    }

    @Transactional
    @Override
    public PasswordResetToken createPasswordResetTokenForUser( User user, String token ) {
        PasswordResetToken userToken = new PasswordResetToken();
        userToken.setUser( user );
        userToken.updateToken( token );
        return passwordResetTokenRepository.save( userToken );
    }

    @Override
    public void verifyPasswordResetToken( int userId, String token ) throws TokenException {
        PasswordResetToken passToken = passwordResetTokenRepository.findByToken( token );
        if ( ( passToken == null ) || ( !passToken.getUser().getId().equals( userId ) ) ) {
            throw new TokenException( "Invalid Token" );
        }

        Calendar cal = Calendar.getInstance();
        if ( ( passToken.getExpiryDate().getTime() - cal.getTime().getTime() ) <= 0 ) {
            throw new TokenException( "Expired" );
        }
    }

    @Transactional
    @Override
    public User changePasswordByResetToken( int userId, String token, String newPassword )
            throws TokenException, ValidationException {

        verifyPasswordResetToken( userId, token );

        if ( newPassword.length() >= 6 ) { //TODO: Tie in with hibernate constraint on User or not necessary?

            // Preauthorize might cause trouble here if implemented, fix by setting manual authentication
            User user = findUserByIdNoAuth( userId );

            user.setPassword( bCryptPasswordEncoder.encode( newPassword ) );
            return updateNoAuth( user );
        } else {
            throw new ValidationException( "Password must be a minimum of 6 characters" );
        }
    }

    @Transactional
    @Override
    public VerificationToken createVerificationTokenForUser( User user, String token ) {
        VerificationToken userToken = new VerificationToken();
        userToken.setUser( user );
        userToken.updateToken( token );
        return tokenRepository.save( userToken );
    }

    @Transactional
    @Override
    public User confirmVerificationToken( String token ) {
        VerificationToken verificationToken = tokenRepository.findByToken( token );
        if ( verificationToken == null ) {
            throw new TokenException( "Invalid Token" );
        }

        Calendar cal = Calendar.getInstance();
        if ( ( verificationToken.getExpiryDate().getTime() - cal.getTime().getTime() ) <= 0 ) {
            throw new TokenException( "Expired" );
        }

        User user = verificationToken.getUser();
        user.setEnabled( true );
        updateNoAuth( user );
        return user;
    }

    @Override
    @Cacheable("chars")
    public SortedSet<String> getChars() {
        return userRepository.findAll().stream()
                .filter(privacyService::checkCurrentUserCanSee)
                .filter(u -> u.getProfile().getLastName() != null && !u.getProfile().getLastName().isEmpty())
                .map(u -> u.getProfile().getLastName().substring(0, 1).toUpperCase())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Transactional
    protected User updateNoAuth( User user ) {
        return userRepository.save( user );
    }

    private Collection<UserTerm> convertTermTypes( Collection<GeneOntologyTerm> goTerms, Taxon taxon,
                                                   Set<? extends Gene> genes ) {
        List<UserTerm> newTerms = new ArrayList<>();
        for ( GeneOntologyTerm goTerm : goTerms ) {
            UserTerm term = convertTermTypes( goTerm, taxon, genes );
            if ( term != null ) {
                newTerms.add( term );
            }
        }
        return newTerms;
    }

    private UserTerm convertTermTypes( GeneOntologyTerm goTerm, Taxon taxon, Set<? extends Gene> genes ) {
        if ( goTerm != null ) {
            UserTerm term = new UserTerm( goTerm, taxon, genes );
            if ( term.getSize() <= applicationSettings.getGoTermSizeLimit() ) {
                return term;
            }
        }

        return null;
    }

    private boolean updateOrInsert( @NonNull User user, @NonNull Gene gene, @NonNull TierType tier, Optional<PrivacyLevelType> privacyLevel ) {
        UserGene existing = user.getUserGenes().get( gene.getGeneId() );

        boolean updated = false;
        if ( existing != null ) {
            // Only set tier because the rest of it's information is updated PreLoad
            updated = !existing.getTier().equals( tier ) || !Optional.ofNullable( existing.getPrivacyLevel() ).equals( privacyLevel );
            existing.setTier( tier );
            existing.setPrivacyLevel( privacyLevel.orElse( null ) );
        } else {
            user.getUserGenes().put( gene.getGeneId(), new UserGene( gene, user, tier, privacyLevel.orElse( null ) ) );
        }

        return updated;
    }

    private Collection<Gene> calculatedGenesInTaxon( User user, Taxon taxon ) {
        return goService.getGenes( user.getTermsByTaxon( taxon ), taxon );
    }

    private boolean removeGenesFromUserByTaxon( User user, Taxon taxon ) {
        return user.getUserGenes().values().removeIf( ga -> ga.getTaxon().equals( taxon ) );
    }

    private boolean removeGenesFromUserByTiersAndTaxon( User user, Taxon taxon, Collection<TierType> tiers ) {
        return user.getUserGenes().values()
                .removeIf( ga -> tiers.contains( ga.getTier() ) && ga.getTaxon().equals( taxon ) );
    }

    private boolean removeTermsFromUserByTaxon( User user, Taxon taxon ) {
        return user.getUserTerms().removeIf( ut -> ut.getTaxon().equals( taxon ) );
    }

}
