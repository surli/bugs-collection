/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.ows11.validation;

import net.opengis.ows11.HTTPType;

/**
 * A sample validator interface for {@link net.opengis.ows11.DCPType}.
 * This doesn't really do anything, and it's not a real EMF artifact.
 * It was generated by the org.eclipse.emf.examples.generator.validator plug-in to illustrate how EMF's code generator can be extended.
 * This can be disabled with -vmargs -Dorg.eclipse.emf.examples.generator.validator=false.
 */
public interface DCPTypeValidator {
    boolean validate();

    boolean validateHTTP(HTTPType value);
}
