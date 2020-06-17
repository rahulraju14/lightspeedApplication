/**
 * Contains all the JSF-style converter classes. Some of these
 * are for converting the text in drop-down selection lists
 * to enum values.  They are often annotated as a class-specific
 * converter, e.g.,
 * '@FacesConverter(forClass=com.lightspeedeps.type.TimecardSubmitType.class)'.
 * These don't require any JSF converter tag to take effect.
 * <p>
 * Others are annotated with an id, which is normally 'lightspeed.'
 * followed by the class name, e.g.,
 * '@FacesConverter(value="lightspeed.DateTimeConverter")'.
 * To use these, the JSF must include a tag such as:
 * '<f:converter converterId="lightspeed.DateTimeConverter"/>'.
 * <p>
 * Most of the classes implement javax.faces.convert.Converter,
 * although a few of them extend one of our own converter classes
 * when they have shared functionality.
 * For example, {@link com.lightspeedeps.web.converter.TimeConverterHold}
 * extends {@link com.lightspeedeps.web.converter.TimeConverterOC}.
 */
package com.lightspeedeps.web.converter;
