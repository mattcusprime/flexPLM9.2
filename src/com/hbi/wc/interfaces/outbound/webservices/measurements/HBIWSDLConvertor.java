package com.hbi.wc.interfaces.outbound.webservices.measurements;

import org.apache.axis.wsdl.WSDL2Java;

public class HBIWSDLConvertor
{
	public static void main(String[] args) 
	{
		String[] a = {"-D", "-s","-Strue","-pcom.hbi.wc.interfaces.outbound.webservices.measurements","hbiMeasurementsWSDL.xml"};
		WSDL2Java.main(a);
	}
}