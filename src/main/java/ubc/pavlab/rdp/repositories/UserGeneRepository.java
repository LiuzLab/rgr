package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.enums.TierType;

import javax.persistence.QueryHint;
import java.util.Collection;
import java.util.Set;

@Repository
public interface UserGeneRepository extends JpaRepository<UserGene, Integer> {
    Integer countByTierIn( Collection<TierType> tiers );

    @Query("select count(distinct geneId) FROM UserGene WHERE tier IN (:tiers)")
    Integer countDistinctGeneByTierIn( @Param("tiers") Collection<TierType> tiers );

    @Query("select count(distinct user) FROM UserGene WHERE taxon = :taxon")
    Integer countDistinctUserByTaxon( @Param("taxon") Taxon taxon );

    @Query("select count(distinct user) FROM UserGene")
    Integer countDistinctUser();

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("select targetGene FROM Ortholog where sourceGene = :source_gene and targetTaxon = :target_taxon ")
    Collection<Integer> findOrthologs(@Param("source_gene") Integer sourceGene, @Param( "target_taxon" ) Integer targetTaxon);

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findByGeneId(int geneId);
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findByGeneIdAndTier(int geneId, TierType tier);
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findByGeneIdAndTierIn(int geneId, Set<TierType> tiers);
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findBySymbolContainingIgnoreCaseAndTaxon(String symbolContaining, Taxon taxon);
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findBySymbolContainingIgnoreCaseAndTaxonAndTier(String symbolContaining, Taxon taxon, TierType tier);
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findBySymbolContainingIgnoreCaseAndTaxonAndTierIn(String symbolContaining, Taxon taxon, Set<TierType> tiers);

    // Return gene IDs of all associations in tiers
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("select distinct geneId FROM UserGene WHERE tier IN (:tiers)")
    Collection<Integer> findDistinctGeneByTierIn( @Param("tiers") Collection<TierType> tiers );

    // Return the reverse-mapped human gene(s) for a given ortholog target. 
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("select sourceGene FROM Ortholog where targetGene IN (:target_gene) ")
    Collection<Integer> findHumanGenesForTarget(@Param("target_gene") Collection<Integer> targetGene);

    // Return all human genes.
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("select geneId FROM UserGene where taxon = '9606'")
    Collection<Integer> findAllHumanGenes();
}
