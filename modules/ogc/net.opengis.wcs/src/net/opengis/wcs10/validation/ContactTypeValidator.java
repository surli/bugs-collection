/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.wcs10.validation;

import net.opengis.wcs10.AddressType;
import net.opengis.wcs10.OnlineResourceType;
import net.opengis.wcs10.TelephoneType;

/**
 * A sample validator interface for {@link net.opengis.wcs10.ContactType}.
 * This doesn't really do anything, and it's not a real EMF artifact.
 * It was generated by the org.eclipse.emf.examples.generator.validator plug-in to illustrate how EMF's code generator can be extended.
 * This can be disabled with -vmargs -Dorg.eclipse.emf.examples.generator.validator=false.
 */
public interface ContactTypeValidator {
    boolean validate();

    boolean validatePhone(TelephoneType value);
    boolean validateAddress(AddressType value);
    boolean validateOnlineResource(OnlineResourceType value);
}
