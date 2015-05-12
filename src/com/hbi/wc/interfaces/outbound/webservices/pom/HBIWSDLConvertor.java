package com.hbi.wc.interfaces.outbound.webservices.pom;

import org.apache.axis.wsdl.WSDL2Java;

public class HBIWSDLConvertor 
{
	public static void main(String[] args) 
	{
		String[] a = {"-D", "-s","-Strue","-pcom.hbi.wc.interfaces.outbound.webservices.pom","hbiPointsOfMeasureWSDL.xml"};
		WSDL2Java.main(a);
	}
}