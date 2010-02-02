package org.atdl4j.ui.impl;

import java.util.List;
import java.util.Vector;

import javax.xml.bind.JAXBException;

import org.atdl4j.atdl.core.EnumPairT;
import org.atdl4j.atdl.core.ParameterT;
import org.atdl4j.atdl.layout.CheckBoxListT;
import org.atdl4j.atdl.layout.ControlT;
import org.atdl4j.atdl.layout.DropDownListT;
import org.atdl4j.atdl.layout.EditableDropDownListT;
import org.atdl4j.atdl.layout.ListItemT;
import org.atdl4j.atdl.layout.MultiSelectListT;
import org.atdl4j.atdl.layout.RadioButtonListT;
import org.atdl4j.atdl.layout.SingleSelectListT;
import org.atdl4j.data.FIXMessageBuilder;
import org.atdl4j.data.InputAndFilterData;
import org.atdl4j.data.TypeConverterFactory;
import org.atdl4j.data.converter.AbstractTypeConverter;
import org.atdl4j.data.fix.PlainFIXMessageBuilder;
import org.atdl4j.data.fix.Tag959Helper;
import org.atdl4j.ui.ControlUI;

/**
 * Abstract class that represents a Parameter SWT Widget. Implements the FIX
 * value getters's methods.
 */
public abstract class AbstractControlUI<E extends Comparable<?>>
		implements ControlUI<E> {

	protected ParameterT parameter;
	protected ControlT control;
	protected AbstractTypeConverter<E> controlConverter;
	protected AbstractTypeConverter<?> parameterConverter;
	
	@SuppressWarnings("unchecked")
	protected void init() throws JAXBException{
		//TODO: do a safe cast AbstractAdapter<?> adapter = (AbstractAdapter<E>) TypeAdapterFactory.create(control);
		controlConverter = (AbstractTypeConverter<E>) TypeConverterFactory.create(control, parameter);
		if (parameter != null) parameterConverter = (AbstractTypeConverter<?>) TypeConverterFactory.create(parameter);
	}

	public String getControlValueAsString() throws JAXBException {
		return controlConverter.convertValueToString(getControlValue());
	}

	public Comparable<?> getControlValueAsComparable() throws JAXBException
	{
		return controlConverter.convertValueToComparable(getControlValue());
	}
	
	public String getParameterValueAsString() throws JAXBException {
		return parameter == null ? null : parameterConverter.convertValueToString(getParameterValue());
	}

	public Comparable<?> getParameterValueAsComparable() throws JAXBException
	{
		return parameter == null ? null : parameterConverter.convertValueToComparable(getParameterValue());
	}
	
	public void setValueAsString(String string) throws JAXBException {
		// TODO: do i also need a Param adapter pass here?
		setValue(controlConverter.convertValueToComparable(string));
	}
	
// 2/1/2010 Scott Atwell differentiate between ControlComparable and ParameterComparable	
//	public Comparable<?> convertStringToComparable(String string) throws JAXBException
//	{
//		if (parameterConverter != null) return parameterConverter.convertValueToComparable(string);
//		else return controlConverter.convertValueToComparable(string);
//	}
	
	public Comparable<?> convertStringToControlComparable(String string) throws JAXBException
	{
		return controlConverter.convertValueToComparable(string);
	}

	public Comparable<?> convertStringToParameterComparable(String string) throws JAXBException
	{
		if (parameterConverter != null) 
			return parameterConverter.convertValueToComparable(string);
		else 
			return null;
	}

	public ParameterT getParameter() {
		return parameter;
	}
	
	public boolean hasParameter() {
		return (this.parameter != null);
	}
		
	public ControlT getControl() {
		return control;
	}
	
	public String getFIXValue() throws JAXBException {
		PlainFIXMessageBuilder builder = new PlainFIXMessageBuilder();
		builder.onStart();
		getFIXValue(builder);
		builder.onEnd();
		return builder.getMessage();
	}
	
	// 2/1/2010 John Shields added
	public String getTooltip() {
	    if (control.getTooltip() != null) return control.getTooltip();
	    else if (parameter != null && parameter.getDescription() != null) return parameter.getDescription();
	    return null;
	}
	
	// Scott Atwell 1/14/2010 made public
	public int getFIXType() throws JAXBException {
		return Tag959Helper.toInteger(getParameter());
	}

	public void getFIXValue(FIXMessageBuilder builder) throws JAXBException {
		String value = getParameterValueAsString();
		if (value != null) {
			if (getParameter().getFixTag() != null) {
//TODO 				builder.onField(getParameter().getFixTag().intValue(), value.toString());
//TODO Scott Atwell 1/31/2010 added (FixTag=0 to indicate valid parameter but DO NOT INCLUDE in FIX Message)
				if ( getParameter().getFixTag().intValue() == 0 )	{
					// ignore
				}
				else {
					builder.onField(getParameter().getFixTag().intValue(), value.toString());
				}
							
			} else {
/***
//TODO Scott Atwell 1/18/2010 BEFORE				
				String name = getParameter().getName();
				String type = Integer.toString(getFIXType());
				builder.onField(958, name);
				builder.onField(959, type);
				builder.onField(960, value.toString());
***/				
				if ( getParameter().getName().startsWith( InputAndFilterData.FIX_DEFINED_FIELD_PREFIX ) )
				{
					// bypass Hidden "standard fields" (eg "FIX_OrderQty")
				}
				else
				{
					String name = getParameter().getName();
					String type = Integer.toString(getFIXType());
					builder.onField(958, name);
					builder.onField(959, type);
					builder.onField(960, value.toString());
				}
			}
		}
	}
	
	// Helper method to lookup a parameter string where the EnumID is matched
	// across the ListItemTs and EnumPairTs
	protected String getEnumWireValue(String enumID){
		if (parameter != null) {
			java.util.List<EnumPairT> enumPairs = parameter.getEnumPair();
			for (EnumPairT enumPair : enumPairs) {
				if (enumPair.getEnumID().equals(enumID)) return enumPair.getWireValue();	
			}
			// throw error?
		}
		return null;
	}
	
	// Helper method to lookup a parameter string where the EnumID is matched
	// across the ListItemTs and EnumPairTs
	protected String getParameterValueAsEnumWireValue(){
		if ( getControlValue() != null )
		{
			return getEnumWireValue(getControlValue().toString());
		}
		else
		{
			return null;
		}
	}
	
	// Helper method to convert MultipleValueChar / MultipleValueString Control values
	// to ParameterValues
	protected String getParameterValueAsMultipleValueString(){
		String value = "";
		if (getControlValue() != null)
		{
			String enumIDs = getControlValue().toString();
			if (enumIDs != null && parameter != null)
			{
				for (String enumID : enumIDs.split("\\s")) {
					if ("".equals(value)) {
						value += getEnumWireValue(enumID);
					} else {
						value += " " + getEnumWireValue(enumID);
					}
				}
			}
		}
		return "".equals(value) ? null : value;
	}
	
	// Helper method to get control ListItems
	protected List<ListItemT> getListItems()
	{		
		if (control instanceof DropDownListT) {
			return ((DropDownListT)control).getListItem();
		} else if (control instanceof EditableDropDownListT) {
			return ((EditableDropDownListT)control).getListItem();
		} else if (control instanceof RadioButtonListT) {
			return ((RadioButtonListT) control).getListItem();
		} else if (control instanceof CheckBoxListT) {
			return ((CheckBoxListT)control).getListItem();
		} else if (control instanceof SingleSelectListT) {
			return ((SingleSelectListT)control).getListItem();
		} else if (control instanceof MultiSelectListT) {
			return ((MultiSelectListT)control).getListItem();
		// TODO: I think Slider should have list items
		// } else if (control instanceof SliderT) {
		//	return new SliderWidget((SliderT)control).getListItem();
		} else {
			// TODO: this should maybe throw a runtime error???
			// return an empty list
			return new Vector<ListItemT>();
		}
	}
}