/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.wfs;

import java.math.BigInteger;
import java.util.Map;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc --> A representation of the model object '<em><b>Get Feature Type</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc -->
 * 
 * A GetFeature element contains one or more Query elements that describe a query operation on one feature type. In response to a GetFeature request,
 * a Web Feature Service must be able to generate a GML3 response that validates using a schema generated by the DescribeFeatureType request. A Web
 * Feature Service may support other possibly non-XML (and even binary) output formats as long as those formats are advertised in the capabilities
 * document. <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.wfs.GetFeatureType#getQuery <em>Query</em>}</li>
 * <li>{@link net.opengis.wfs.GetFeatureType#getMaxFeatures <em>Max Features</em>}</li>
 * <li>{@link net.opengis.wfs.GetFeatureType#getOutputFormat <em>Output Format</em>}</li>
 * <li>{@link net.opengis.wfs.GetFeatureType#getResultType <em>Result Type</em>}</li>
 * <li>{@link net.opengis.wfs.GetFeatureType#getTraverseXlinkDepth <em>Traverse Xlink Depth</em>}</li>
 * <li>{@link net.opengis.wfs.GetFeatureType#getTraverseXlinkExpiry <em>Traverse Xlink Expiry</em>}</li>
 * <li>{@link net.opengis.wfs.GetFeatureType#getFormatOptions <em>Format Options</em>}</li>
 * <li>{@link net.opengis.wfs.GetFeatureType#getMetadata <em>Metadata</em>}</li>
 * <li>{@link net.opengis.wfs.GetFeatureType#getStartIndex <em>Start Index</em>}</li>
 * <li>{@link net.opengis.wfs.GetFeatureType#getViewParams <em>View Params</em>}</li>
 * </ul>
 * </p>
 * 
 * @see net.opengis.wfs.WfsPackage#getGetFeatureType()
 * @model extendedMetaData="name='GetFeatureType' kind='elementOnly'"
 * @generated
 */
public interface GetFeatureType extends BaseRequestType {
    /**
     * Returns the value of the '<em><b>Query</b></em>' containment reference list. The list contents are of type {@link net.opengis.wfs.QueryType}.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Query</em>' containment reference list isn't clear, there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     * 
     * @return the value of the '<em>Query</em>' containment reference list.
     * @see net.opengis.wfs.WfsPackage#getGetFeatureType_Query()
     * @model type="net.opengis.wfs.QueryType" containment="true" required="true"
     *        extendedMetaData="kind='element' name='Query' namespace='##targetNamespace'"
     * @generated
     */
    EList getQuery();

    /**
     * Returns the value of the '<em><b>Max Features</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc -->
     * 
     * The maxFeatures attribute is used to specify the maximum number of features that a GetFeature operation should generate (regardless of the
     * actual number of query hits). <!-- end-model-doc -->
     * 
     * @return the value of the '<em>Max Features</em>' attribute.
     * @see #setMaxFeatures(BigInteger)
     * @see net.opengis.wfs.WfsPackage#getGetFeatureType_MaxFeatures()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.PositiveInteger" extendedMetaData="kind='attribute' name='maxFeatures'"
     * @generated
     */
    BigInteger getMaxFeatures();

    /**
     * Sets the value of the '{@link net.opengis.wfs.GetFeatureType#getMaxFeatures <em>Max Features</em>}' attribute. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @param value the new value of the '<em>Max Features</em>' attribute.
     * @see #getMaxFeatures()
     * @generated
     */
    void setMaxFeatures(BigInteger value);

    /**
     * The initial index of a feature result set in which to return features.
     * <p>
     * This property is coupled with {@link #getMaxFeatures()} to page through a feature result set.
     * </p>
     * 
     * @model
     */
    BigInteger getStartIndex();

    /**
     * Sets the value of the '{@link net.opengis.wfs.GetFeatureType#getStartIndex <em>Start Index</em>}' attribute. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @param value the new value of the '<em>Start Index</em>' attribute.
     * @see #getStartIndex()
     * @generated
     */
    void setStartIndex(BigInteger value);

    /**
     * Returns the value of the '<em><b>Output Format</b></em>' attribute. The default value is <code>"text/xml; subtype=gml/3.1.1"</code>. <!--
     * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc -->
     * 
     * The outputFormat attribute is used to specify the output format that the Web Feature Service should generate in response to a GetFeature or
     * GetFeatureWithLock element. The default value of 'text/xml; subtype=gml/3.1.1' indicates that the output is an XML document that conforms to
     * the Geography Markup Language (GML) Implementation Specification V3.1.1. For the purposes of experimentation, vendor extension, or even
     * extensions that serve a specific community of interest, other acceptable output format values may be used to specify other formats as long as
     * those values are advertised in the capabilities document. For example, the value WKB may be used to indicate that a Well Known Binary format be
     * used to encode the output. <!-- end-model-doc -->
     * 
     * @return the value of the '<em>Output Format</em>' attribute.
     * @see #isSetOutputFormat()
     * @see #unsetOutputFormat()
     * @see #setOutputFormat(String)
     * @see net.opengis.wfs.WfsPackage#getGetFeatureType_OutputFormat()
     * @model default="text/xml; subtype=gml/3.1.1" unique="false" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.String"
     *        extendedMetaData="kind='attribute' name='outputFormat'"
     * @generated
     */
    String getOutputFormat();

    /**
     * Sets the value of the '{@link net.opengis.wfs.GetFeatureType#getOutputFormat <em>Output Format</em>}' attribute. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @param value the new value of the '<em>Output Format</em>' attribute.
     * @see #isSetOutputFormat()
     * @see #unsetOutputFormat()
     * @see #getOutputFormat()
     * @generated
     */
    void setOutputFormat(String value);

    /**
     * Unsets the value of the '{@link net.opengis.wfs.GetFeatureType#getOutputFormat <em>Output Format</em>}' attribute. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @see #isSetOutputFormat()
     * @see #getOutputFormat()
     * @see #setOutputFormat(String)
     * @generated
     */
    void unsetOutputFormat();

    /**
     * Returns whether the value of the '{@link net.opengis.wfs.GetFeatureType#getOutputFormat <em>Output Format</em>}' attribute is set. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @return whether the value of the '<em>Output Format</em>' attribute is set.
     * @see #unsetOutputFormat()
     * @see #getOutputFormat()
     * @see #setOutputFormat(String)
     * @generated
     */
    boolean isSetOutputFormat();

    /**
     * Returns the value of the '<em><b>Result Type</b></em>' attribute. The default value is <code>"results"</code>. The literals are from the
     * enumeration {@link net.opengis.wfs.ResultTypeType}. <!-- begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc -->
     * 
     * The resultType attribute is used to indicate what response a WFS should return to user once a GetFeature request is processed. Possible values
     * are: results - meaning that the full response set (i.e. all the feature instances) should be returned. hits - meaning that an empty response
     * set should be returned (i.e. no feature instances should be returned) but the "numberOfFeatures" attribute should be set to the number of
     * feature instances that would be returned. <!-- end-model-doc -->
     * 
     * @return the value of the '<em>Result Type</em>' attribute.
     * @see net.opengis.wfs.ResultTypeType
     * @see #isSetResultType()
     * @see #unsetResultType()
     * @see #setResultType(ResultTypeType)
     * @see net.opengis.wfs.WfsPackage#getGetFeatureType_ResultType()
     * @model default="results" unique="false" unsettable="true" extendedMetaData="kind='attribute' name='resultType'"
     * @generated
     */
    ResultTypeType getResultType();

    /**
     * Sets the value of the '{@link net.opengis.wfs.GetFeatureType#getResultType <em>Result Type</em>}' attribute. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @param value the new value of the '<em>Result Type</em>' attribute.
     * @see net.opengis.wfs.ResultTypeType
     * @see #isSetResultType()
     * @see #unsetResultType()
     * @see #getResultType()
     * @generated
     */
    void setResultType(ResultTypeType value);

    /**
     * Unsets the value of the '{@link net.opengis.wfs.GetFeatureType#getResultType <em>Result Type</em>}' attribute. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @see #isSetResultType()
     * @see #getResultType()
     * @see #setResultType(ResultTypeType)
     * @generated
     */
    void unsetResultType();

    /**
     * Returns whether the value of the '{@link net.opengis.wfs.GetFeatureType#getResultType <em>Result Type</em>}' attribute is set. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @return whether the value of the '<em>Result Type</em>' attribute is set.
     * @see #unsetResultType()
     * @see #getResultType()
     * @see #setResultType(ResultTypeType)
     * @generated
     */
    boolean isSetResultType();

    /**
     * Returns the value of the '<em><b>Traverse Xlink Depth</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc
     * -->
     * 
     * This attribute indicates the depth to which nested property XLink linking element locator attribute (href) XLinks are traversed and resolved if
     * possible. A value of "1" indicates that one linking element locator attribute (href) Xlink will be traversed and the referenced element
     * returned if possible, but nested property XLink linking element locator attribute (href) XLinks in the returned element are not traversed. A
     * value of " " indicates that all nested property XLink linking element locator attribute (href) XLinks will be traversed and the referenced
     * elements returned if possible. The range of valid values for this attribute consists of positive integers plus " ". If this attribute is not
     * specified then no xlinks shall be resolved and the value of traverseXlinkExpiry attribute (if it specified) may be ignored. <!-- end-model-doc
     * -->
     * 
     * @return the value of the '<em>Traverse Xlink Depth</em>' attribute.
     * @see #setTraverseXlinkDepth(String)
     * @see net.opengis.wfs.WfsPackage#getGetFeatureType_TraverseXlinkDepth()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData="kind='attribute' name='traverseXlinkDepth'"
     * @generated
     */
    String getTraverseXlinkDepth();

    /**
     * Sets the value of the '{@link net.opengis.wfs.GetFeatureType#getTraverseXlinkDepth <em>Traverse Xlink Depth</em>}' attribute. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @param value the new value of the '<em>Traverse Xlink Depth</em>' attribute.
     * @see #getTraverseXlinkDepth()
     * @generated
     */
    void setTraverseXlinkDepth(String value);

    /**
     * Returns the value of the '<em><b>Traverse Xlink Expiry</b></em>' attribute. <!-- begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc
     * -->
     * 
     * The traverseXlinkExpiry attribute value is specified in minutes. It indicates how long a Web Feature Service should wait to receive a response
     * to a nested GetGmlObject request. This attribute is only relevant if a value is specified for the traverseXlinkDepth attribute. <!--
     * end-model-doc -->
     * 
     * @return the value of the '<em>Traverse Xlink Expiry</em>' attribute.
     * @see #setTraverseXlinkExpiry(BigInteger)
     * @see net.opengis.wfs.WfsPackage#getGetFeatureType_TraverseXlinkExpiry()
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.PositiveInteger" extendedMetaData="kind='attribute' name='traverseXlinkExpiry'"
     * @generated
     */
    BigInteger getTraverseXlinkExpiry();

    /**
     * Sets the value of the '{@link net.opengis.wfs.GetFeatureType#getTraverseXlinkExpiry <em>Traverse Xlink Expiry</em>}' attribute. <!--
     * begin-user-doc --> <!-- end-user-doc -->
     * 
     * @param value the new value of the '<em>Traverse Xlink Expiry</em>' attribute.
     * @see #getTraverseXlinkExpiry()
     * @generated
     */
    void setTraverseXlinkExpiry(BigInteger value);

    /**
     * The format options to be applied to any response to the GetFeature requst.
     * <p>
     * This property is not part of the standard model but an extension.
     * </p>
     * 
     * @model
     */
    Map getFormatOptions();

    /**
     * Sets the value of the '{@link net.opengis.wfs.GetFeatureType#getFormatOptions <em>Format Options</em>}' attribute. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @param value the new value of the '<em>Format Options</em>' attribute.
     * @see #getFormatOptions()
     * @generated
     */
    void setFormatOptions(Map value);

    /**
     * A generic bag of extra information that implementations can use to carry vendor parameters
     * <p>
     * This property is not part of the standard model but an extension.
     * </p>
     * 
     * @model
     */
    Map getMetadata();

    /**
     * Sets the value of the '{@link net.opengis.wfs.GetFeatureType#getMetadata <em>Metadata</em>}' attribute. <!-- begin-user-doc --> <!--
     * end-user-doc -->
     * 
     * @param value the new value of the '<em>Metadata</em>' attribute.
     * @see #getMetadata()
     * @generated
     */
    void setMetadata(Map value);

    /**
     * A list of maps containing "View Parameters" to be plugged into the request
     * <p>
     * This property is not part of the standard model but an extension.
     * </p>
     * 
     * @model type="java.util.Map" unique="false" extendedMetaData="kind='attribute' namespace='##targetNamespace'"
     */
    EList getViewParams();

} // GetFeatureType
