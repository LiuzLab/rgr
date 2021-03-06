package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.model.PrivacySensitive;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * These methods help determining what content are authorized to see from other users given their privacy preferences.
 */
public interface PrivacyService {

    /**
     * Check of a given user has access to a privacy-sensitive content.
     *
     * @param user
     * @param content
     * @return
     */
    boolean checkUserCanSee( User user, PrivacySensitive content );

    /**
     * Check if the current user has access to a given privacy-sensitive content.
     *
     * @param content
     * @return
     */
    boolean checkCurrentUserCanSee( PrivacySensitive content );

    /**
     * Check if a privacy level is enabled in the configuration.
     *
     * @param privacyLevel
     * @return
     */
    boolean isPrivacyLevelEnabled( PrivacyLevelType privacyLevel );

    boolean isGenePrivacyLevelEnabled( PrivacyLevelType privacyLevel );

    PrivacyLevelType getDefaultPrivacyLevel();

    /**
     * @param user
     * @param international
     * @return
     */
    boolean checkUserCanSearch( User user, boolean international );

    boolean checkCurrentUserCanSearch( boolean international );
}
