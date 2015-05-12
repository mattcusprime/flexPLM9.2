package com.hbi.wc.interfaces.outbound.webservices.processor;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Locale;

import com.hbi.wc.interfaces.outbound.webservices.pom.ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean;
import com.hbi.wc.interfaces.outbound.webservices.pom.IESoapServletLocator;
import com.hbi.wc.interfaces.outbound.webservices.pom.SoapBindingStub;
import com.hbi.wc.interfaces.outbound.webservices.util.HBIIntegrationClientUtil;
import com.hbi.wc.interfaces.outbound.webservices.util.HBIProperties;
import com.lcs.wc.db.FlexObject;
import com.lcs.wc.flextype.FlexType;
import com.lcs.wc.foundation.LCSLifecycleManaged;
import com.lcs.wc.foundation.LCSLifecycleManagedHelper;
import com.lcs.wc.foundation.LCSQuery;
import com.lcs.wc.measurements.LCSMeasurements;
import com.lcs.wc.measurements.LCSMeasurementsMaster;
import com.lcs.wc.measurements.LCSPointsOfMeasure;
import com.lcs.wc.product.LCSProduct;
import com.lcs.wc.season.LCSSeason;
import com.lcs.wc.sourcing.LCSSourcingConfig;
import com.lcs.wc.specification.FlexSpecQuery;
import com.lcs.wc.specification.FlexSpecification;
import com.lcs.wc.util.FormatHelper;
import com.lcs.wc.util.LCSLog;
import com.lcs.wc.util.LCSProperties;
import com.lcs.wc.util.VersionHelper;

import wt.method.MethodContext;
import wt.method.RemoteAccess;
import wt.method.RemoteMethodServer;
import wt.session.SessionContext;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;

/**
 * HBIPointsOfMeasureDataProcessor.java
 * 
 * This class is using as a Processor(which will invoke data sync tool from source to target system) to process all pending transactions & perform different actions in target server
 * @author Abdul.Patel@Hanes.com
 * @since  April-10-2015
 */
public class HBIPointsOfMeasureDataProcessor implements RemoteAccess
{
	private static String CLIENT_ADMIN_USER_ID = "";
	private static String CLIENT_ADMIN_PASSWORD = "";
	private static String SERVER_ADMIN_USER_ID = "";
	private static String SERVER_ADMIN_PASSWORD = "";
	private static RemoteMethodServer remoteMethodServer;
	
	static
	{
		try
		{
			CLIENT_ADMIN_USER_ID = LCSProperties.get("com.hbi.wc.integration.CLIENT_ADMIN_USER_ID", "Administrator");
			CLIENT_ADMIN_PASSWORD = LCSProperties.get("com.hbi.wc.integration.CLIENT_ADMIN_PASSWORD", "admin1");
			SERVER_ADMIN_USER_ID = LCSProperties.get("com.hbi.wc.integration.SERVER_ADMIN_USER_ID", "Administrator");
			SERVER_ADMIN_PASSWORD = LCSProperties.get("com.hbi.wc.integration.SERVER_ADMIN_PASSWORD", "c9admin");
		}
		catch (Exception exp)
		{
			LCSLog.debug("Exception in static block of the class HBIPointsOfMeasureDataProcessor is :: " + exp);
		}
	}
	
	/** Default executable function of the class HBIPointsOfMeasureDataProcessor */
	public static void main(String[] args)
	{
		LCSLog.debug("### START HBIPointsOfMeasureDataProcessor.main() ###");
		try
		{
			MethodContext mcontext = new MethodContext((String) null, (Object) null);
			SessionContext sessioncontext = SessionContext.newContext();

			remoteMethodServer = RemoteMethodServer.getDefault();
			remoteMethodServer.setUserName(CLIENT_ADMIN_USER_ID);
			remoteMethodServer.setPassword(CLIENT_ADMIN_PASSWORD);
			
			//remoteMethodServer.invoke("invokePointsOfMeasureDataProcessor", "com.hbi.wc.interfaces.outbound.webservices.processor.HBIPointsOfMeasureDataProcessor", null, null, null);
			
			//calling a function which will validate and sync PointsOfMeasure(Template and Instance) data from source FlexPLM to target FlexPLM based on the user actions on source PLM
			invokePointsOfMeasureDataProcessor();
			System.exit(0);
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
			System.exit(1);
		}
		
		LCSLog.debug("### END HBIPointsOfMeasureDataProcessor.main() ###");
	}
	
	/**
	 * This function is using to initialize Service Locator and invoke SOAP Protocol with java bean(contains all attributes data) to perform create or update action in target server
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */
	public static void invokePointsOfMeasureDataProcessor() throws WTException, WTPropertyVetoException
	{
		LCSLog.debug("### START HBIPointsOfMeasureDataProcessor.invokePointsOfMeasureDataProcessor() ###");
		String flexObjectClassName = "com.lcs.wc.measurements.LCSPointsOfMeasure";
		LCSLifecycleManaged transactionObj = null;
		
		try
		{
			//Initializing Service Locator and invoking SOAP Protocol with server details, preparing input bean object & calling invoke function to perform action in target server
			IESoapServletLocator serviceLocator = new IESoapServletLocator();
			SoapBindingStub stub = (SoapBindingStub) serviceLocator.getIESoapPort();
			stub.setUsername(SERVER_ADMIN_USER_ID);
			stub.setPassword(SERVER_ADMIN_PASSWORD);
			stub._setProperty("sendMultiRefs", new Boolean(false));
			
			//Calling a function to get 'Pending' Transaction object from the pre-defined 'Business Object' path for the given FlexObject class name and other internal parameters
			Collection<FlexObject> hbiTransactionObjectCollection = new HBIIntegrationClientUtil().getHBITransactionBusinessObjectForPendingStatus(flexObjectClassName);
			for(FlexObject flexObj : hbiTransactionObjectCollection)
			{
				transactionObj = (LCSLifecycleManaged) LCSQuery.findObjectById("OR:com.lcs.wc.foundation.LCSLifecycleManaged:"+ flexObj.getString("LCSLIFECYCLEMANAGED.IDA2A2"));
				if(transactionObj != null)
				{
					//Calling a function to initialize LCSMeasurements from the given Transaction object, prepare java bean from the LCSMeasurements object and invoke stub function
					new HBIPointsOfMeasureDataProcessor().invokePointsOfMeasureDataProcessor(stub, transactionObj);
				}
			}
		}
		catch (Exception exp)
		{
			//Updating the given transactionObj(instance of LCSLifecycleManaged) for 'Comments' field to provide the reason for not processing the Transaction successfully
			transactionObj.setValue(HBIProperties.hbiCommentsKey, "Manual Intervention needed:- Exception in Processing Transaction object is :: "+ exp.getMessage());
			transactionObj.setValue(HBIProperties.hbiTransactionStatusKey, "failed");
			LCSLifecycleManagedHelper.getService().saveLifecycleManaged(transactionObj);
			exp.printStackTrace();
		}
		
		LCSLog.debug("### END HBIPointsOfMeasureDataProcessor.invokePointsOfMeasureDataProcessor() ###");
	}
	
	/**
	 * This function is using to initialize LCSPointsOfMeasure from the given LCSLifecycleManaged instance, prepare java bean from the LCSPointsOfMeasure object & invoke stub function
	 * @param stub - SoapBindingStub
	 * @param transactionObj - LCSLifecycleManaged
	 * @return transactionObj - LCSLifecycleManaged
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 * @throws RemoteException
	 */
	public LCSLifecycleManaged invokePointsOfMeasureDataProcessor(SoapBindingStub stub, LCSLifecycleManaged transactionObj) throws WTException, WTPropertyVetoException, RemoteException
	{
		// LCSLog.debug("### START HBIPointsOfMeasureDataProcessor.invokePointsOfMeasureDataProcessor(stub, transactionObj) ###");
		String flexObjectOID = (String) transactionObj.getValue(HBIProperties.hbiFlexObjectOIDKey);
		LCSPointsOfMeasure pointsOfMeasureObj = (LCSPointsOfMeasure) LCSQuery.findObjectById(flexObjectOID);
		
		//Validating the given object, invoking various internal functions for validation and data preparation, invoke stub function for data sync and update transaction object
		if(pointsOfMeasureObj != null)
		{
			//Get Latest version of the given LCSPointsOfMeasure, validate transaction object status, invoke LCSPointsOfMeasure data sync process and update transaction object status
			pointsOfMeasureObj = new HBIIntegrationClientUtil().getLatestPointsOfMeasureFor(pointsOfMeasureObj);
			String transactionObjectComments = new HBIIntegrationClientUtil().isValidPointsOfMeasureTransactionObject(transactionObj, pointsOfMeasureObj);
			if(FormatHelper.hasContent(transactionObjectComments))
			{
				transactionObj.setValue(HBIProperties.hbiCommentsKey, transactionObjectComments);
				transactionObj.setValue(HBIProperties.hbiTransactionStatusKey, "failed");
			}
			else
			{
				transactionObj = invokePointsOfMeasureDataProcessor(stub, pointsOfMeasureObj, transactionObj);
			}
		}
		else
		{
			//Updating the given transactionObj(instance of LCSLifecycleManaged) for 'Comments' field to provide the reason for not processing the Transaction successfully
			transactionObj.setValue(HBIProperties.hbiCommentsKey, "Manual Intervention needed:- Issue in forming LCSPointsOfMeasure object for the given OID :: "+ flexObjectOID);
			transactionObj.setValue(HBIProperties.hbiTransactionStatusKey, "failed");
		}
		
		//Updating the given transactionObj(instance of LCSLifecycleManaged) to mark 'Transaction Status' as 'Completed/Failed' as this is Transaction is processed for data sync
		transactionObj = LCSLifecycleManagedHelper.getService().saveLifecycleManaged(transactionObj);
		
		// LCSLog.debug("### END HBIPointsOfMeasureDataProcessor.invokePointsOfMeasureDataProcessor(stub, transactionObj) ###");
		return transactionObj;
	}
	
	/**
	 * This function is using to validate the transaction object status, invoke transaction object data sync process and update the given transaction object comments and process status
	 * @param stub - SoapBindingStub
	 * @param pointsOfMeasureObj - LCSPointsOfMeasure
	 * @param transactionObj - LCSLifecycleManaged
	 * @return transactionObj - LCSLifecycleManaged
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 * @throws RemoteException
	 */
	public LCSLifecycleManaged invokePointsOfMeasureDataProcessor(SoapBindingStub stub, LCSPointsOfMeasure pointsOfMeasureObj, LCSLifecycleManaged transactionObj) throws WTException, WTPropertyVetoException, RemoteException
	{
		// LCSLog.debug("### START HBIPointsOfMeasureDataProcessor.invokePointsOfMeasureDataProcessor(pointsOfMeasureObj, transactionObj) ###");
		boolean isvalidTransactionObject = true;
		
		//Validate the Transaction object status only if the PointsOfMeasure is of type INSTANCE/INSTANCE_SPECIFIC, because these two states are coming from Product PointsOfMeasure
		String pointsOfMeasureType = pointsOfMeasureObj.getPointsOfMeasureType();
		if("INSTANCE".equalsIgnoreCase(pointsOfMeasureType) || "INSTANCE_SPECIFIC".equalsIgnoreCase(pointsOfMeasureType))
		{
			isvalidTransactionObject = new HBIIntegrationClientUtil().isValidTransactionObject(transactionObj, pointsOfMeasureObj.getMeasurements());
		}
			
		//if the Transaction object is valid, then invoke Transaction object data sync process, else populate the suitable error message in comments field and skip object process 
		if(isvalidTransactionObject)
		{
			ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean hbiPointsOfMeasureBean = invokePointsOfMeasureDataProcessor(pointsOfMeasureObj, pointsOfMeasureType);
			stub.hbiPointsOfMeasureTask(hbiPointsOfMeasureBean);
			transactionObj.setValue(HBIProperties.hbiCommentsKey, "");
			transactionObj.setValue(HBIProperties.hbiTransactionStatusKey, "completed");
		}
		else
		{
			transactionObj.setValue(HBIProperties.hbiCommentsKey, "This is a dummy transaction object created for PointsOfMeasure instnace data which is other than Block Products, hence marking as completed without processing to VRD");
			transactionObj.setValue(HBIProperties.hbiTransactionStatusKey, "completed");
		}
		
		// LCSLog.debug("### END HBIPointsOfMeasureDataProcessor.invokePointsOfMeasureDataProcessor(pointsOfMeasureObj, transactionObj) ###");
		return transactionObj;
	}
	
	/**
	 * This function is using to identify pointsOfMeasureType, initialize PointsOfMeasure bean, populate all attributes data based on the pointsOfMeasureType & return updated instance
	 * @param pointsOfMeasureObj - LCSPointsOfMeasure
	 * @param pointsOfMeasureType - String
	 * @return hbiPointsOfMeasureBean - ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 * @throws RemoteException
	 */
	public ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean invokePointsOfMeasureDataProcessor(LCSPointsOfMeasure pointsOfMeasureObj, String pointsOfMeasureType) throws WTException, WTPropertyVetoException, RemoteException
	{
		// LCSLog.debug("### START HBIPointsOfMeasureDataProcessor.invokePointsOfMeasureDataProcessor(pointsOfMeasureObj) ###");
		
		//Calling a function which is using to get all attributes(fields) data from the given LCSPointsOfMeasure instance, initialize PointsOfMeasure bean & return from function header
		ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean hbiPointsOfMeasureBean = updatePointsOfMeasureBeanForSimpleAttributes(pointsOfMeasureObj);
		
		if("TEMPLATE".equalsIgnoreCase(pointsOfMeasureType) || "GRADINGS".equalsIgnoreCase(pointsOfMeasureType))
		{
			String measurementsName = pointsOfMeasureObj.getMeasurements().getMeasurementsName();
			hbiPointsOfMeasureBean.setMeasurementsName(measurementsName);
		}
		else if("INSTANCE".equalsIgnoreCase(pointsOfMeasureType) || "INSTANCE_SPECIFIC".equalsIgnoreCase(pointsOfMeasureType))
		{
			//Calling a function to populate logical attributes data(like Season Name, Product Name, Product Pattern No, SourcingConfig Name & Specification Name) to the given object
			hbiPointsOfMeasureBean = updatePointsOfMeasureBeanForLogicalAttributes(hbiPointsOfMeasureBean, pointsOfMeasureObj);
			
			//Get Measurements object from the given PointsOfMeasure, get valid Measurement Name from Measurements instance and update Measurement Name to the PointsOfMeasure Bean
			LCSMeasurements measurementsObj = pointsOfMeasureObj.getMeasurements();
			String measurementsName = new HBIMeasurementsDataProcessor().getMeasurementsName(measurementsObj, measurementsObj.getMeasurementsType());
			hbiPointsOfMeasureBean.setMeasurementsName(measurementsName);
		}
		
		// LCSLog.debug("### END HBIPointsOfMeasureDataProcessor.invokePointsOfMeasureDataProcessor(pointsOfMeasureObj) ###");
		return hbiPointsOfMeasureBean;
	}
	
	/**
	 * This function is using to get all attributes(fields) data from the given LCSPointsOfMeasure instance, initialize PointsOfMeasure bean object and populate simple attributes data 
	 * @param pointsOfMeasureObj - LCSPointsOfMeasure
	 * @return hbiPointsOfMeasureBean - ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */
	public ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean updatePointsOfMeasureBeanForSimpleAttributes(LCSPointsOfMeasure pointsOfMeasureObj) throws WTException, WTPropertyVetoException
	{
		// LCSLog.debug("### START HBIPointsOfMeasureDataProcessor.updatePointsOfMeasureBeanForSimpleAttributes(pointsOfMeasureObj) ###");
		
		//Get attributes data from the given LCSPointsOfMeasure, format/downcast each attributes data(Like Attributes of ATT, NUM and DATE) as needed
		String hbiGradeCode = (String) pointsOfMeasureObj.getValue(HBIProperties.hbiGradeCodeKey);
		String htmInstruction = ""+(String) pointsOfMeasureObj.getValue(HBIProperties.htmInstructionKey);
		String measurementName = ""+(String) pointsOfMeasureObj.getValue(HBIProperties.measurementNameKey);
		String sampleMeasurementComments = ""+ pointsOfMeasureObj.getValue(HBIProperties.sampleMeasurementCommentsKey);
		String number = ""+(String) pointsOfMeasureObj.getValue(HBIProperties.numberKey);
		String newMeasurement = Double.toString((Double) pointsOfMeasureObj.getValue(HBIProperties.newMeasurementKey)); 
		String plusTolerance = Double.toString((Double)pointsOfMeasureObj.getValue(HBIProperties.plusToleranceKey));
		String minusTolerance = Double.toString((Double)pointsOfMeasureObj.getValue(HBIProperties.minusToleranceKey));
		String imageName = ""+(String) pointsOfMeasureObj.getValue(HBIProperties.howToMeasureKey);
		
		//Initialize PointsOfMeasure bean object instance and update all attributes data to the initialized bean object and return updated bean object from the function header
		ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean hbiPointsOfMeasureBean = new ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean();
		hbiPointsOfMeasureBean.setInstanceType(pointsOfMeasureObj.getPointsOfMeasureType());
		hbiPointsOfMeasureBean.setHbiGradeCode(hbiGradeCode);
		hbiPointsOfMeasureBean.setHtmInstruction(htmInstruction);
		hbiPointsOfMeasureBean.setMeasurementName(measurementName);
		hbiPointsOfMeasureBean.setSampleMeasurementComments(sampleMeasurementComments);
		hbiPointsOfMeasureBean.setNumber(number);
		hbiPointsOfMeasureBean.setNewMeasurement(newMeasurement);
		hbiPointsOfMeasureBean.setPlusTolerance(plusTolerance);
		hbiPointsOfMeasureBean.setMinusTolerance(minusTolerance);
		hbiPointsOfMeasureBean.setImageName(imageName);
		
		//calling a function to get LCSPointsOfMeasure Type(Library/Template/Instance/Instance_Specific), initialize Object Identifier, and populate the identifier on POMBean object
		hbiPointsOfMeasureBean = updatePointsOfMeasureBeanForPOMIdentifier(hbiPointsOfMeasureBean, pointsOfMeasureObj);
		
		//calling a function which is using to populate Single List Attribute(populate AttributeList-Value for keys) data for the given PointsOfMeasure Bean and LCSPointsOfMeasure
		hbiPointsOfMeasureBean = updatePointsOfMeasureBeanForSingleListAttributes(hbiPointsOfMeasureBean, pointsOfMeasureObj);
		
		return hbiPointsOfMeasureBean;
		// LCSLog.debug("### END HBIPointsOfMeasureDataProcessor.updatePointsOfMeasureBeanForSimpleAttributes(pointsOfMeasureObj) ###");
	}
	
	/**
	 * This function is using to update all logical attributes data(like Season Name, Product Name, Product Pattern No, SourcingConfig Name and Specification Name) to the given object
	 * @param hbiPointsOfMeasureBean - ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean
	 * @param pointsOfMeasureObj - LCSPointsOfMeasure
	 * @return hbiPointsOfMeasureBean - ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */
	@SuppressWarnings("unchecked")
	public ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean updatePointsOfMeasureBeanForLogicalAttributes(ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean hbiPointsOfMeasureBean, LCSPointsOfMeasure pointsOfMeasureObj) throws WTException, WTPropertyVetoException
	{
		// LCSLog.debug("### END HBIPointsOfMeasureDataProcessor.updatePointsOfMeasureBeanForLogicalAttributes(hbiPointsOfMeasureBean, pointsOfMeasureObj) ###");
		LCSMeasurements measurementsObj = pointsOfMeasureObj.getMeasurements();
		Collection<FlexObject> measurementComponentsCollection = FlexSpecQuery.componentWhereUsed((LCSMeasurementsMaster)measurementsObj.getMaster());
		
		//validating the Collection<FlexObject> (contains set of components associated with the given measurements) and initializing component object needed for measurements bean
		if(measurementComponentsCollection != null && measurementComponentsCollection.size() > 0 )
		{
			FlexObject flexObj = measurementComponentsCollection.iterator().next();
				
			//Forming LCSSeason from 'Measurements' Instance data(get all Components from the given Measurement), which is using as a parameter to process requested action in VRD
			LCSSeason seasonObj = (LCSSeason) LCSQuery.findObjectById("VR:com.lcs.wc.season.LCSSeason:"+flexObj.getString("SPECTOLATESTITERSEASON.SEASONBRANCHID")); 
			seasonObj = (LCSSeason) VersionHelper.latestIterationOf(seasonObj);
			hbiPointsOfMeasureBean.setSeasonName(seasonObj.getName());
					
			//Forming LCSProduct from 'Measurements' Instance data(get all Components from the given Measurement), which is using as a parameter to process requested action in VRD
			LCSProduct productObj = (LCSProduct) LCSQuery.findObjectById("VR:com.lcs.wc.product.LCSProduct:"+flexObj.getString("LCSPRODUCT.BRANCHIDITERATIONINFO"));
			productObj = (LCSProduct) VersionHelper.latestIterationOf(productObj);
			hbiPointsOfMeasureBean.setProductName(productObj.getName());
			hbiPointsOfMeasureBean.setHbiPatternNo((String) productObj.getValue(HBIProperties.hbiPatternNoKey));
					
			//Forming SourcingConfig from 'Measurements' Instance data(get all Components from the given Measurement), which is using as a parameter to process requested action in VRD
			LCSSourcingConfig sourcingConfigObj = (LCSSourcingConfig) LCSQuery.findObjectById("VR:com.lcs.wc.sourcing.LCSSourcingConfig:"+flexObj.getString("LCSSOURCINGCONFIG.BRANCHIDITERATIONINFO"));
			sourcingConfigObj = (LCSSourcingConfig) VersionHelper.latestIterationOf(sourcingConfigObj);
			hbiPointsOfMeasureBean.setSourcingConfigName(sourcingConfigObj.getSourcingConfigName());
					
			//Forming FlexSpecification from 'Measurements' Instance data(get all Components from the given Measurement), using as a parameter to process requested action in VRD
			FlexSpecification flexSpecObj = (FlexSpecification) LCSQuery.findObjectById("VR:com.lcs.wc.specification.FlexSpecification:"+flexObj.getString("LATESTITERFLEXSPECIFICATION.BRANCHIDITERATIONINFO"));
			flexSpecObj = (FlexSpecification) VersionHelper.latestIterationOf(flexSpecObj);
			hbiPointsOfMeasureBean.setSpecificationName(flexSpecObj.getName());
		}
		
		// LCSLog.debug("### END HBIPointsOfMeasureDataProcessor.updatePointsOfMeasureBeanForLogicalAttributes(hbiPointsOfMeasureBean, pointsOfMeasureObj) ###");
		return hbiPointsOfMeasureBean;
	}
	
	/**
	 * This function is using to get LCSPointsOfMeasure Type(Library/Template/Instance/Instance_Specific), initialize Object Identifier, and populate the identifier on POMBean object
	 * @param hbiPointsOfMeasureBean - ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean
	 * @param pointsOfMeasureObj - LCSPointsOfMeasure
	 * @return hbiPointsOfMeasureBean - ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */
	private ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean updatePointsOfMeasureBeanForPOMIdentifier(ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean hbiPointsOfMeasureBean, LCSPointsOfMeasure pointsOfMeasureObj) throws WTException, WTPropertyVetoException
	{
		// LCSLog.debug("### START HBIPointsOfMeasureDataProcessor.updatePointsOfMeasureBeanForPOMIdentifier(hbiPointsOfMeasureBean, pointsOfMeasureObj) ###");
		String pointsOfMeasureType = pointsOfMeasureObj.getPointsOfMeasureType();
		String flexObjectId = FormatHelper.getNumericObjectIdFromObject(pointsOfMeasureObj);
		
		//Get LCSPointsOfMeasure Type(Library/Template/Instance/Instance_Specific) and Initialize Object Id from LCSPointsOfMeasure instance, using to populate on POMBean object
		if(!"LIBRARY".equalsIgnoreCase(pointsOfMeasureType))
		{
			LCSPointsOfMeasure createEventPOMObj = new HBIIntegrationClientUtil().getLatestOrOldPointsOfMeasureFor(pointsOfMeasureObj, true, false);
			if(createEventPOMObj != null)
			{
				flexObjectId = FormatHelper.getNumericObjectIdFromObject(createEventPOMObj);
			}
			
			//Updating Measurement Bean with the given LCSPointsOfMeasure instance sorting number, which is using to maintain the sorting number in target(FlexPLM 10.1) server
			String sortingNumber = Integer.toString(pointsOfMeasureObj.getSortingNumber());
			hbiPointsOfMeasureBean.setSortingNumber(sortingNumber);
		}
		
		//Updating LCSPointsOfMeasure FlexObjectId to the given hbiPointsOfMeasureBean instance, which is using in target system in order to determine the event type(create/update)
		hbiPointsOfMeasureBean.setHbiPOMIntegrationIdentifier(flexObjectId);
		
		//Validate the given LCSPointsOfMeasure instance drop status and populate the transaction object event type(perform create/update/delete event) for target FlexPLM server
		if(pointsOfMeasureObj.isDropped())
		{
			hbiPointsOfMeasureBean.setEventType("delete");
		}
		
		// LCSLog.debug("### END HBIPointsOfMeasureDataProcessor.updatePointsOfMeasureBeanForPOMIdentifier(hbiPointsOfMeasureBean, pointsOfMeasureObj) ###");
		return hbiPointsOfMeasureBean;
	}
	
	/**
	 * This function is using to populate Single List Attribute(populate AttributeList-Value for keys) data for the given PointsOfMeasure Bean instance from LCSPointsOfMeasure object 
	 * @param hbiPointsOfMeasureBean - ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean
	 * @param pointsOfMeasureObj - LCSPointsOfMeasure
	 * @return hbiPointsOfMeasureBean - ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */
	public ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean updatePointsOfMeasureBeanForSingleListAttributes(ComHbiWcInterfacesOutboundWebservicesPomHBIPointsOfMeasureBean hbiPointsOfMeasureBean, LCSPointsOfMeasure pointsOfMeasureObj) throws WTException, WTPropertyVetoException
	{
		// LCSLog.debug("### START HBIPointsOfMeasureDataProcessor.updatePointsOfMeasureBeanForSingleListAttributes(hbiPointsOfMeasureBean, pointsOfMeasureObj) ###");
		FlexType pointsOfMeasureFlexTypeObj = pointsOfMeasureObj.getFlexType();
		
		//Get 'Placement Amount' Single List Attribute-Key(Pick list key), get Value from the AttributeList-Key and update AttributeList-Value to the given Measurement Bean instance
		String placementAmount = (String) pointsOfMeasureObj.getValue(HBIProperties.placementAmountKey);
		if(FormatHelper.hasContent(placementAmount))
		{
			placementAmount = pointsOfMeasureFlexTypeObj.getAttribute(HBIProperties.placementAmountKey).getAttValueList().getValue(placementAmount, Locale.getDefault());
			hbiPointsOfMeasureBean.setPlacementAmount(placementAmount);
		}
		
		//Get 'Placement Reference' Single List Attribute-Key(Pick list key), get Value from the AttributeList-Key & update AttributeList-Value to the given Measurement Bean instance
		String placementReference = (String) pointsOfMeasureObj.getValue(HBIProperties.placementReferenceKey);
		if(FormatHelper.hasContent(placementReference))
		{
			placementReference = pointsOfMeasureFlexTypeObj.getAttribute(HBIProperties.placementReferenceKey).getAttValueList().getValue(placementReference, Locale.getDefault());
			hbiPointsOfMeasureBean.setPlacementReference(placementReference);
		}
		
		// LCSLog.debug("### END HBIPointsOfMeasureDataProcessor.updatePointsOfMeasureBeanForSingleListAttributes(hbiPointsOfMeasureBean, pointsOfMeasureObj) ###");
		return hbiPointsOfMeasureBean;
	}
}
