package org.opensrp.connector.openmrs.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opensrp.common.util.HttpResponse;
import org.opensrp.common.util.HttpUtil;
import org.opensrp.connector.openmrs.schedule.OpenmrsSyncerListener;
import org.opensrp.domain.Client;
import org.opensrp.domain.Event;
import org.opensrp.domain.Obs;
import org.opensrp.domain.User;
import org.opensrp.service.ClientService;
import org.opensrp.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mysql.jdbc.StringUtils;

@Service
public class EncounterService extends OpenmrsService {
	private static Logger logger = LoggerFactory.getLogger(EncounterService.class.toString());
	
	private static final String ENCOUNTER_URL = "ws/rest/v1/encounter";//"ws/rest/emrapi/encounter";
	
	private static final String BAHMNI_ENCOUNTER_URL = "ws/rest/v1/bahmnicore/bahmniencounter";
	
	private static final String OBS_URL = "ws/rest/v1/obs";
	
	private static final String ENCOUNTER__TYPE_URL = "ws/rest/v1/encountertype";
	
	public static final String OPENMRS_UUID_IDENTIFIER_TYPE = "OPENMRS_UUID";
	
	private PatientService patientService;
	
	private OpenmrsUserService userService;
	
	private ClientService clientService;
	
	private EventService eventService;
	
	@Autowired
	private OpenmrsLocationService openmrsLocationService;
	
	@Autowired
	public EncounterService(PatientService patientService, OpenmrsUserService userService, ClientService clientService,
	    EventService eventService) {
		this.patientService = patientService;
		this.userService = userService;
		this.clientService = clientService;
		this.eventService = eventService;
	}
	
	public EncounterService(String openmrsUrl, String user, String password) {
		super(openmrsUrl, user, password);
	}
	
	public PatientService getPatientService() {
		return patientService;
	}
	
	public void setPatientService(PatientService patientService) {
		this.patientService = patientService;
	}
	
	public OpenmrsUserService getUserService() {
		return userService;
	}
	
	public void setUserService(OpenmrsUserService userService) {
		this.userService = userService;
	}
	
	public JSONObject getEncounterByUuid(String uuid, boolean noRepresentationTag) throws JSONException {
		return new JSONObject(HttpUtil.get(getURL() + "/" + ENCOUNTER_URL + "/" + uuid, noRepresentationTag ? "" : "v=full",
		    OPENMRS_USER, OPENMRS_PWD).body());
	}
	
	public JSONObject getBahmniEncounterByUuid(String uuid, boolean noRepresentationTag) throws JSONException {
		return new JSONObject(HttpUtil.get(getURL() + "/" + BAHMNI_ENCOUNTER_URL + "/" + uuid,
		    noRepresentationTag ? "" : "v=full", OPENMRS_USER, OPENMRS_PWD).body());
	}
	
	public JSONObject getObsByEncounterUuid(String encounterUuid) throws JSONException {
		// The data format returned contains the obs uuid and concept uuids
		return new JSONObject(HttpUtil.get(getURL() + "/" + ENCOUNTER_URL + "/" + encounterUuid,
		    "v=custom:(uuid,obs:(uuid,concept:(uuid)))", OPENMRS_USER, OPENMRS_PWD).body());
	}
	
	public JSONObject getObsUuidByParentObsUuid(String obsUuid) throws JSONException {
		//The data format returned contains the children obs uuid and concept uuids
		return new JSONObject(HttpUtil.get(getURL() + "/" + OBS_URL + "/" + obsUuid,
		    "v=custom:(groupMembers:(uuid,concept:(uuid)))", OPENMRS_USER, OPENMRS_PWD).body());
	}
	
	public JSONObject getEncounterType(String encounterType) throws JSONException {
		// we have to use this ugly approach because identifier not found throws exception and 
		// its hard to find whether it was network error or object not found or server error
		JSONArray res = new JSONObject(HttpUtil.get(getURL() + "/" + ENCOUNTER__TYPE_URL, "v=full", OPENMRS_USER,
		    OPENMRS_PWD).body()).getJSONArray("results");
		for (int i = 0; i < res.length(); i++) {
			if (res.getJSONObject(i).getString("display").equalsIgnoreCase(encounterType)) {
				return res.getJSONObject(i);
			}
		}
		return null;
	}
	
	public JSONObject createEncounterType(String name, String description) throws JSONException {
		JSONObject o = convertEncounterToOpenmrsJson(name, description);
		return new JSONObject(HttpUtil.post(getURL() + "/" + ENCOUNTER__TYPE_URL, "", o.toString(), OPENMRS_USER,
		    OPENMRS_PWD).body());
	}
	
	public JSONObject convertEncounterToOpenmrsJson(String name, String description) throws JSONException {
		JSONObject a = new JSONObject();
		a.put("name", name);
		a.put("description", description);
		return a;
	}
	
	public JSONObject createEncounter(Event e) throws JSONException {
		JSONObject enc = createEncounterJson(e);
		logger.info("\n \n \n Final JSON <<>> <<>> <<>>" + enc.toString()+"\n \n \n");
		//JSONObject enc2 = new JSONObject("{\"locationUuid\":\"7d76f621-63ce-4bbb-a715-176286f8de84\",\"patientUuid\":\"b0e47458-1bcb-42d4-88d7-f5aaaa2aa1af\",\"encounterUuid\":null,\"visitUuid\":null,\"providers\":[{\"uuid\":\"313c8507-9821-40e4-8a70-71a5c7693d72\"}],\"encounterDateTime\":null,\"extensions\":{\"mdrtbSpecimen\":[]},\"context\":{},\"visitType\":\"OPD\",\"bahmniDiagnoses\":[],\"orders\":[],\"drugOrders\":[],\"disposition\":null,\"observations\":[{\"concept\":{\"uuid\":\"f87fe2f4-de90-4c48-b07d-4050a5debf8c\",\"name\":\"\u09B8\u09CD\u09AC\u09BE\u09B8\u09CD\u09A5\u09CD\u09AF \u09B8\u09C7\u09AC\u09BE:\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"\u09B8\u09BE\u09A7\u09BE\u09B0\u09A8 \u09B0\u09C7\u09BE\u0997\u09C0\u09B0 \u09B8\u09C7\u09AC\u09BE \u09AB\u09B0\u09CD\u09AE.12/39-0\",\"voided\":false,\"value\":{\"uuid\":\"c2bb6edf-18cb-4c7f-ad91-7c8dd561a437\",\"name\":{\"display\":\"\u0989\u099A\u09CD\u099A \u09B0\u0995\u09CD\u09A4\u099A\u09BE\u09AA\",\"uuid\":\"38b6a8b8-e02f-41c7-813d-63df201562a6\",\"name\":\"\u0989\u099A\u09CD\u099A \u09B0\u0995\u09CD\u09A4\u099A\u09BE\u09AA\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"links\":[{\"rel\":\"self\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/c2bb6edf-18cb-4c7f-ad91-7c8dd561a437/name/38b6a8b8-e02f-41c7-813d-63df201562a6\"},{\"rel\":\"full\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/c2bb6edf-18cb-4c7f-ad91-7c8dd561a437/name/38b6a8b8-e02f-41c7-813d-63df201562a6?v=full\"}],\"resourceVersion\":\"1.9\"},\"names\":[{\"display\":\"\u0989\u099A\u09CD\u099A \u09B0\u0995\u09CD\u09A4\u099A\u09BE\u09AA\",\"uuid\":\"38b6a8b8-e02f-41c7-813d-63df201562a6\",\"name\":\"\u0989\u099A\u09CD\u099A \u09B0\u0995\u09CD\u09A4\u099A\u09BE\u09AA\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"links\":[{\"rel\":\"self\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/c2bb6edf-18cb-4c7f-ad91-7c8dd561a437/name/38b6a8b8-e02f-41c7-813d-63df201562a6\"},{\"rel\":\"full\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/c2bb6edf-18cb-4c7f-ad91-7c8dd561a437/name/38b6a8b8-e02f-41c7-813d-63df201562a6?v=full\"}],\"resourceVersion\":\"1.9\"},{\"display\":\"High Blood Pressure\",\"uuid\":\"98a2f125-8eac-4680-bd08-33572ff023d0\",\"name\":\"High Blood Pressure\",\"locale\":\"en\",\"localePreferred\":false,\"conceptNameType\":\"FULLY_SPECIFIED\",\"links\":[{\"rel\":\"self\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/c2bb6edf-18cb-4c7f-ad91-7c8dd561a437/name/98a2f125-8eac-4680-bd08-33572ff023d0\"},{\"rel\":\"full\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/c2bb6edf-18cb-4c7f-ad91-7c8dd561a437/name/98a2f125-8eac-4680-bd08-33572ff023d0?v=full\"}],\"resourceVersion\":\"1.9\"}],\"displayString\":\"\u0989\u099A\u09CD\u099A \u09B0\u0995\u09CD\u09A4\u099A\u09BE\u09AA\",\"resourceVersion\":\"2.0\",\"translationKey\":\"\u0989\u099A\u09CD\u099A_\u09B0\u0995\u09CD\u09A4\u099A\u09BE\u09AA_39\"},\"inactive\":false,\"groupMembers\":[]},{\"concept\":{\"uuid\":\"9be72615-b914-4d35-ac80-d5953744a9d0\",\"name\":\"\u09B0\u09C7\u09AB\u09BE\u09B0\u09C7\u09B2:\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"\u09B8\u09BE\u09A7\u09BE\u09B0\u09A8 \u09B0\u09C7\u09BE\u0997\u09C0\u09B0 \u09B8\u09C7\u09AC\u09BE \u09AB\u09B0\u09CD\u09AE.12/40-0\",\"voided\":false,\"value\":{\"uuid\":\"1e3f1870-b252-4808-8edb-f86fad050ebd\",\"name\":{\"display\":\"\u09A1\u09BE\u09AF\u09BC\u09BE\u09AC\u09C7\u099F\u09BF\u09B8\",\"uuid\":\"befce65b-9e80-45ec-b8b7-05234cd5cb9c\",\"name\":\"\u09A1\u09BE\u09AF\u09BC\u09BE\u09AC\u09C7\u099F\u09BF\u09B8\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"links\":[{\"rel\":\"self\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/1e3f1870-b252-4808-8edb-f86fad050ebd/name/befce65b-9e80-45ec-b8b7-05234cd5cb9c\"},{\"rel\":\"full\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/1e3f1870-b252-4808-8edb-f86fad050ebd/name/befce65b-9e80-45ec-b8b7-05234cd5cb9c?v=full\"}],\"resourceVersion\":\"1.9\"},\"names\":[{\"display\":\"Diabetes\",\"uuid\":\"fdabcf86-7ac9-4122-96f7-9f84858228fd\",\"name\":\"Diabetes\",\"locale\":\"en\",\"localePreferred\":false,\"conceptNameType\":\"FULLY_SPECIFIED\",\"links\":[{\"rel\":\"self\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/1e3f1870-b252-4808-8edb-f86fad050ebd/name/fdabcf86-7ac9-4122-96f7-9f84858228fd\"},{\"rel\":\"full\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/1e3f1870-b252-4808-8edb-f86fad050ebd/name/fdabcf86-7ac9-4122-96f7-9f84858228fd?v=full\"}],\"resourceVersion\":\"1.9\"},{\"display\":\"\u09A1\u09BE\u09AF\u09BC\u09BE\u09AC\u09C7\u099F\u09BF\u09B8\",\"uuid\":\"befce65b-9e80-45ec-b8b7-05234cd5cb9c\",\"name\":\"\u09A1\u09BE\u09AF\u09BC\u09BE\u09AC\u09C7\u099F\u09BF\u09B8\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"links\":[{\"rel\":\"self\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/1e3f1870-b252-4808-8edb-f86fad050ebd/name/befce65b-9e80-45ec-b8b7-05234cd5cb9c\"},{\"rel\":\"full\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/1e3f1870-b252-4808-8edb-f86fad050ebd/name/befce65b-9e80-45ec-b8b7-05234cd5cb9c?v=full\"}],\"resourceVersion\":\"1.9\"}],\"displayString\":\"\u09A1\u09BE\u09AF\u09BC\u09BE\u09AC\u09C7\u099F\u09BF\u09B8\",\"resourceVersion\":\"2.0\",\"translationKey\":\"\u09A1\u09BE\u09AF\u09BC\u09BE\u09AC\u09C7\u099F\u09BF\u09B8_40\"},\"inactive\":false,\"groupMembers\":[]},{\"concept\":{\"uuid\":\"514de0ad-14e3-4fc6-b4a3-a2683317ab53\",\"name\":\"\u09B8\u09CD\u09AC\u09BE\u09B8\u09CD\u09A5\u09CD\u09AF \u09B6\u09BF\u0995\u09CD\u09B7\u09BE:\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"\u09B8\u09BE\u09A7\u09BE\u09B0\u09A8 \u09B0\u09C7\u09BE\u0997\u09C0\u09B0 \u09B8\u09C7\u09AC\u09BE \u09AB\u09B0\u09CD\u09AE.12/41-0\",\"voided\":false,\"value\":{\"uuid\":\"0622f52f-0c95-41c1-ab5d-ee9bc335c839\",\"name\":{\"display\":\"\u09B8\u09AE\u09CD\u09AD\u09BE\u09AC\u09CD\u09AF \u09AF\u0995\u09CD\u09B7\u09CD\u09AE\u09BE\",\"uuid\":\"cc994816-e03d-4674-a455-f1087b88e934\",\"name\":\"\u09B8\u09AE\u09CD\u09AD\u09BE\u09AC\u09CD\u09AF \u09AF\u0995\u09CD\u09B7\u09CD\u09AE\u09BE\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"links\":[{\"rel\":\"self\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/0622f52f-0c95-41c1-ab5d-ee9bc335c839/name/cc994816-e03d-4674-a455-f1087b88e934\"},{\"rel\":\"full\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/0622f52f-0c95-41c1-ab5d-ee9bc335c839/name/cc994816-e03d-4674-a455-f1087b88e934?v=full\"}],\"resourceVersion\":\"1.9\"},\"names\":[{\"display\":\"Tuberculosis\",\"uuid\":\"9c02fdc7-0e51-4a1f-a530-586bfed811a9\",\"name\":\"Tuberculosis\",\"locale\":\"en\",\"localePreferred\":false,\"conceptNameType\":\"FULLY_SPECIFIED\",\"links\":[{\"rel\":\"self\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/0622f52f-0c95-41c1-ab5d-ee9bc335c839/name/9c02fdc7-0e51-4a1f-a530-586bfed811a9\"},{\"rel\":\"full\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/0622f52f-0c95-41c1-ab5d-ee9bc335c839/name/9c02fdc7-0e51-4a1f-a530-586bfed811a9?v=full\"}],\"resourceVersion\":\"1.9\"},{\"display\":\"\u09B8\u09AE\u09CD\u09AD\u09BE\u09AC\u09CD\u09AF \u09AF\u0995\u09CD\u09B7\u09CD\u09AE\u09BE\",\"uuid\":\"cc994816-e03d-4674-a455-f1087b88e934\",\"name\":\"\u09B8\u09AE\u09CD\u09AD\u09BE\u09AC\u09CD\u09AF \u09AF\u0995\u09CD\u09B7\u09CD\u09AE\u09BE\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"links\":[{\"rel\":\"self\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/0622f52f-0c95-41c1-ab5d-ee9bc335c839/name/cc994816-e03d-4674-a455-f1087b88e934\"},{\"rel\":\"full\",\"uri\":\"http://192.168.19.44/openmrs/ws/rest/v1/concept/0622f52f-0c95-41c1-ab5d-ee9bc335c839/name/cc994816-e03d-4674-a455-f1087b88e934?v=full\"}],\"resourceVersion\":\"1.9\"}],\"displayString\":\"\u09B8\u09AE\u09CD\u09AD\u09BE\u09AC\u09CD\u09AF \u09AF\u0995\u09CD\u09B7\u09CD\u09AE\u09BE\",\"resourceVersion\":\"2.0\",\"translationKey\":\"\u09B8\u09AE\u09CD\u09AD\u09BE\u09AC\u09CD\u09AF_\u09AF\u0995\u09CD\u09B7\u09CD\u09AE\u09BE_41\"},\"inactive\":false,\"groupMembers\":[]}],\"encounterTypeUuid\":\"81852aee-3f10-11e4-adec-0800271c1b75\"}\r\n");
		HttpResponse op = HttpUtil.post(HttpUtil.removeEndingSlash(OPENMRS_BASE_URL) + "/" + BAHMNI_ENCOUNTER_URL, "",
		    enc.toString(), OPENMRS_USER, OPENMRS_PWD);
		logger.info("\n \n \n"+ "Response From Openmrs <<>> <<>> <<>>" + op.body()+"\n \n \n");
		return new JSONObject(op.body());
	}

	private JSONObject createEncounterJson(Event e) throws JSONException {
		JSONObject pt = patientService.getPatientByIdentifier(e.getBaseEntityId());
		Client client = clientService.getByBaseEntityId(e.getBaseEntityId());
		JSONObject enc = getStaticJsonObject("normalDisease");
		enc.put("patientUuid", pt.getString("uuid"));
		enc.put("locationUuid", e.getLocationId());
		JSONObject pr = userService.getPersonByUser(e.getProviderId());
		JSONArray dummnyArray = new JSONArray();
		
/*		//Main JSON Structure
		//enc.put("encounterDatetime", OPENMRS_DATE.format(e.getEventDate().toDate()));
		enc.put("encounterDatetime", JSONObject.NULL);
		// patient must be existing in OpenMRS before it submits an encounter. if it doesnot it would throw NPE
		enc.put("patientUuid", pt.getString("uuid"));
		enc.put("locationUuid", e.getLocationId());
		//enc.put("provider", pr.getString("uuid"));
		//TODO enc.put("patientUuid", pt.getString("uuid"));
		//enc.put("encounterType", e.getEventType());
		//TODO enc.put("encounterTypeUuid", e.getEventType());
		JSONObject dummbyObject = new JSONObject();
		//enc.put("context", dummbyObject);
		enc.put("context", JSONObject.NULL);
		enc.put("encounterUuid", JSONObject.NULL);
		enc.put("visitUuid", JSONObject.NULL);
		enc.put("disposition", JSONObject.NULL);
		enc.put("orders", dummnyArray);
		enc.put("drugOrders", dummnyArray);
		enc.put("bahmniDiagnoses", dummnyArray);
		JSONArray prov = new JSONArray();
		JSONObject providerUUID = new JSONObject();
		providerUUID.put("uuid", "313c8507-9821-40e4-8a70-71a5c7693d72");
		prov.put(providerUUID);
		enc.put("providers", prov);
		enc.put("visitType", "Community clinic service");
		enc.put("encounterTypeUuid", "81852aee-3f10-11e4-adec-0800271c1b75");
		dummbyObject.put("mdrtbSpecimen", dummnyArray);
		enc.put("extensions", dummbyObject);*/
		
		//observations for Followup Disease Female and Male
		JSONArray obar = null;
		if (e.getEventType().equalsIgnoreCase("Followup Disease Female")) {
			obar = createObservationNormalDisease(e);
		}else if (e.getEventType().equalsIgnoreCase("Followup Family Planning")) {
			obar = createObservationFamilyPlanning(e);
		}else if (e.getEventType().equalsIgnoreCase("Followup ANC")) {
			obar = createObservationFollowupANC(e);
		}else if (e.getEventType().equalsIgnoreCase("Followup PNC")) {
			obar = createObservationFollowupPNC(e);
		}else if (e.getEventType().equalsIgnoreCase("Followup Disease Child")) {
			obar = createObservationFollowupDiseaseChild(e);
		}else if (e.getEventType().equalsIgnoreCase("Followup Disease Toddler")) {
			obar = createObservationFollowupDiseaseToddler(e);
		}
		enc.put("observations", obar);
		return enc;
	}
	
	private JSONArray createObservationFollowupDiseaseToddler(Event e)
			throws JSONException {
		JSONArray obar= new JSONArray();
		List<String> diseaseList = null;
		String formFieldPath = "শিশু (২ মাস থেকে ৫ বছর) স্বাস্থ্য সেবা.32/61-0";
		Client client = clientService.getByBaseEntityId(e.getBaseEntityId());
		boolean hasDisease =false;
		if(client.getAttributes().containsKey("has_disease")){
			String hasDiseaseStr = (String)client.getAttributes().get("has_disease");
			if(hasDiseaseStr.equals("হ্যাঁ")){
				hasDisease = true;
			}
		}
		if(client.getAttributes().containsKey("Disease_status") && hasDisease == true){
			String diseaseString = (String)client.getAttributes().get("Disease_status");
			diseaseList = Arrays.asList(diseaseString.split(","));
		}
		if(hasDisease){
			JSONObject healthCareGivenYes = getStaticJsonObject("healthCareGivenYes");
			healthCareGivenYes.put("formFieldPath", formFieldPath);
			//JSONObject concept = staticJSONObject.getJSONObject("concept");
			JSONObject concept = new JSONObject();
			concept.put("name", "Disease_2Months_To_5Years_CHCP");
			concept.put("uuid", "ed6dedbf-7bd3-4642-b497-0535e3ee1986");
			obar.put(healthCareGivenYes);
			if(diseaseList!=null){
				//for(String diseaseName : diseaseList){
				for(int i=0; i< diseaseList.size()-1; i++){
					String diseaseName = diseaseList.get(i);
					if(diseaseName!= null && !diseaseName.isEmpty()){
						if(diseaseName.equals("Pneumonia") || diseaseName.equals("unspec.")){
							String nextDiseaseName = diseaseList.get(i+1);
							nextDiseaseName = nextDiseaseName.trim();
							logger.info("\n\n\n<><><><><> "+ diseaseName +" --> "+nextDiseaseName+ "<><><><><>\n\n\n ");
							
							if(diseaseName.equals("Pneumonia")){
								if(nextDiseaseName.equals("unspec.")){
									//JSONObject staticJSONObject = getStaticJsonObject("coldAndCough");
									obar =addDiseaseInObservationArray("coldAndCough", obar, formFieldPath, concept);
									i++;
								}else{
									obar =addDiseaseInObservationArray(diseaseName, obar, formFieldPath, concept);
								}
							}
						}else{
							obar =addDiseaseInObservationArray(diseaseName, obar, formFieldPath, concept);
						}
					}
				}
			}
		}else{
			JSONObject healthCareGivenNo = getStaticJsonObject("healthCareGivenNo");
			healthCareGivenNo.put("formFieldPath", formFieldPath);
			obar.put(healthCareGivenNo);
		}
		obar = addRefferedPlaceInObservationArray(e, obar, formFieldPath);
		return obar;
	}
	
	private JSONArray addDiseaseInObservationArray(String diseaseName, JSONArray obar, String formFieldPath, JSONObject concept) throws JSONException{
		JSONObject staticJSONObject = getStaticJsonObject(diseaseName);
		logger.info("\n\n\n<><><><><>"+formFieldPath+"-->"+diseaseName+"-->"+ staticJSONObject + "<><><><><>\n\n\n ");
		if(staticJSONObject!= null){
			staticJSONObject.put("concept", concept);
			staticJSONObject.put("formFieldPath", formFieldPath);
			obar.put(staticJSONObject);
		}
		return obar;
	}
	
	private JSONArray createObservationFollowupDiseaseChild(Event e)
			throws JSONException {
		JSONArray obar= new JSONArray();
		List<String> diseaseList = null;
		String formFieldPath = "শিশু (০ থেকে ২ মাস) স্বাস্থ্য সেবা.35/73-0";
		Client client = clientService.getByBaseEntityId(e.getBaseEntityId());
		boolean hasDisease =false;
		if(client.getAttributes().containsKey("has_disease")){
			String hasDiseaseStr = (String)client.getAttributes().get("has_disease");
			if(hasDiseaseStr.equals("হ্যাঁ")){
				hasDisease = true;
			}
		}
		if(client.getAttributes().containsKey("Disease_status") && hasDisease == true){
			String diseaseString = (String)client.getAttributes().get("Disease_status");
			diseaseList = Arrays.asList(diseaseString.split(","));
		}
		if(hasDisease){
			JSONObject healthCareGivenYes = getStaticJsonObject("healthCareGivenYes");
			healthCareGivenYes.put("formFieldPath", formFieldPath);
			//JSONObject concept = staticJSONObject.getJSONObject("concept");
			JSONObject concept = new JSONObject();
			concept.put("name", "Disease_Below_2Month_CHCP");
			concept.put("uuid", "1031ee9f-460c-433d-b0f9-e6aac203d857");
			obar.put(healthCareGivenYes);
			if(diseaseList!=null){
				//for(String diseaseName : diseaseList){
				for(int i=0; i< diseaseList.size()-1; i++){
					String diseaseName = diseaseList.get(i);
					if(diseaseName!= null && !diseaseName.isEmpty()){
						if(diseaseName.equals("Pneumonia") || diseaseName.equals("unspec.")){
							String nextDiseaseName = diseaseList.get(i+1);
							nextDiseaseName = nextDiseaseName.trim();
							logger.info("\n\n\n<><><><><> "+ diseaseName +" --> "+nextDiseaseName+ "<><><><><>\n\n\n ");
							
							if(diseaseName.equals("Pneumonia")){
								if(nextDiseaseName.equals("unspec.")){
									JSONObject staticJSONObject = getStaticJsonObject("coldAndCough");
									logger.info("\n\n\n<><><><><> Child disease static JSON :"+"coldAndCough"+"->>"+ staticJSONObject + "<><><><><>\n\n\n ");
									if(staticJSONObject!= null){
										staticJSONObject.put("formFieldPath", formFieldPath);
										obar.put(staticJSONObject);
									}
									i++;
								}else{
									JSONObject staticJSONObject = getStaticJsonObject(diseaseName);
									logger.info("\n\n\n<><><> Child disease static JSON :"+diseaseName+"->>"+ staticJSONObject + "<><><><><>\n\n\n ");
									if(staticJSONObject!= null){
										staticJSONObject.put("formFieldPath", formFieldPath);
										obar.put(staticJSONObject);
									}
								}
							}
						}else{
							JSONObject staticJSONObject = getStaticJsonObject(diseaseName);
							logger.info("\n\n\n<><><><><> Child disease static JSON :"+diseaseName+"->>"+ staticJSONObject + "<><><><><>\n\n\n ");
							if(staticJSONObject!= null){
								staticJSONObject.put("concept", concept);
								staticJSONObject.put("formFieldPath", formFieldPath);
								obar.put(staticJSONObject);
							}
						}
					}
				}
			}
		}else{
			JSONObject healthCareGivenNo = getStaticJsonObject("healthCareGivenNo");
			healthCareGivenNo.put("formFieldPath", formFieldPath);
			obar.put(healthCareGivenNo);
		}
		
		obar = addRefferedPlaceInObservationArray(e, obar, formFieldPath);
		return obar;
	}
	
	
	private JSONArray createObservationFollowupPNC(Event e)
			throws JSONException {
		JSONArray obar= new JSONArray();
		String formFieldPath = "প্রসব পরবর্তী সেবা.43/52-0";
		Client client = clientService.getByBaseEntityId(e.getBaseEntityId());
		obar = addRefferedPlaceInObservationArray(e, obar,formFieldPath);
		return obar;
	}

	private JSONArray createObservationFollowupANC(Event e)
			throws JSONException {
		JSONArray obar= new JSONArray();
		String formFieldPath = "প্রসব পূর্ব সেবা.86/134-0";
		Client client = clientService.getByBaseEntityId(e.getBaseEntityId());
		if(client.getAttributes().containsKey("Denger_Signs_During_Pregnancy")){
			String dangerSignsDuringPregnancyString = (String)client.getAttributes().get("Denger_Signs_During_Pregnancy");
			List<String> dangerSignsDuringPregnancyList = Arrays.asList(dangerSignsDuringPregnancyString.split(","));
			if(dangerSignsDuringPregnancyList.size()>0){
				//"formFieldPath": "প্রসব পূর্ব সেবা.86/134-0"
				JSONObject healthCareGivenYes = getStaticJsonObject("healthCareGivenYes");
				healthCareGivenYes.put("formFieldPath", formFieldPath);
				obar.put(healthCareGivenYes);
				obar.put(getStaticJsonObject("haveDangerSignsPregnancyYes"));
				for(String dangerSign : dangerSignsDuringPregnancyList){
					JSONObject staticJSONObject = getStaticJsonObject(dangerSign);
					logger.info("\n\n\n<><><><><> Danger sign static JSON :"+dangerSign+"->>"+ staticJSONObject + "<><><><><>\n\n\n ");
					if(staticJSONObject!= null){
						obar.put(staticJSONObject);
					}
				}	
			}else{
				JSONObject healthCareGivenNo = getStaticJsonObject("healthCareGivenNo");
				healthCareGivenNo.put("formFieldPath", formFieldPath);
				obar.put(healthCareGivenNo);
			}
		}else{
			JSONObject healthCareGivenNo = getStaticJsonObject("healthCareGivenNo");
			healthCareGivenNo.put("formFieldPath", formFieldPath);
			obar.put(healthCareGivenNo);
		}
		obar = addRefferedPlaceInObservationArray(e, obar,formFieldPath);
		return obar;
	}
	
	private JSONArray createObservationFamilyPlanning(Event e)
			throws JSONException {
		JSONArray obar= new JSONArray();
		Client client = clientService.getByBaseEntityId(e.getBaseEntityId());
		if(client.getAttributes().containsKey("familyplanning")){
			String familyPlanning = (String)client.getAttributes().get("familyplanning");
			familyPlanning = familyPlanning.trim();
			logger.info("\n\n\n<><><><><><><><> Family Planning Process :"+ familyPlanning + "<><><><><><><><>\n\n\n ");
			if(familyPlanning.equals("খাবার বড়ি")){
				JSONObject familyPlanningCHCP = createJsonFamilyPlanningCHCP("oralContraceptives");
				obar.put(familyPlanningCHCP);
			}else if(familyPlanning.equals("কনডম")){
				JSONObject familyPlanningCHCP = createJsonFamilyPlanningCHCP("condoms");
				obar.put(familyPlanningCHCP);
			}else if(familyPlanning.equals("ইনজেক্টবল")){
				JSONObject familyPlanningCHCP = createJsonFamilyPlanningCHCP("injectable");
				obar.put(familyPlanningCHCP);
			}else if(familyPlanning.equals("অন্যান্য পদ্ধতি")){
				JSONObject familyPlanningCHCP = createJsonFamilyPlanningCHCP("otherMethod");
				obar.put(familyPlanningCHCP);
			}
		}
		return obar;
	}
	
	private JSONObject createJsonFamilyPlanningCHCP(String processName) throws JSONException{
		logger.info("\n\n\n<><><><><><><><> Family Planning Process in createJSON function :"+ processName + "<><><><><><><><>\n\n\n ");
		JSONObject familyPlanningCHCP = getStaticJsonObject("familyPlanningCHCP");
		JSONArray groupMembers= new JSONArray();
		groupMembers.put(getStaticJsonObject(processName));
		familyPlanningCHCP.put("groupMembers", groupMembers);
		logger.info("\n \n \n Final JSON <<>> <<>> <<>>" + familyPlanningCHCP.toString()+"\n \n \n");
		return familyPlanningCHCP;
	}
	
	private JSONArray createObservationNormalDisease(Event e)
			throws JSONException {
		JSONArray obar= new JSONArray();
		List<String> diseaseList = null;
		String formFieldPath = "সাধারন রোগীর সেবা.19/43-0";
		Client client = clientService.getByBaseEntityId(e.getBaseEntityId());
		boolean hasDisease =false;
		if(client.getAttributes().containsKey("has_disease")){
			String hasDiseaseStr = (String)client.getAttributes().get("has_disease");
			if(hasDiseaseStr.equals("হ্যাঁ")){
				hasDisease = true;
			}
		}
		if(client.getAttributes().containsKey("Disease_status") && hasDisease == true){
			String diseaseString = (String)client.getAttributes().get("Disease_status");
			diseaseList = Arrays.asList(diseaseString.split(","));
		}
		if(hasDisease){
			obar.put(getStaticJsonObject("healthCareGivenYes"));
			if(diseaseList!=null){
				for(String diseaseName : diseaseList){
					if(diseaseName.equals("High Blood Pressure")){
						obar.put(getStaticJsonObject("highBloodPressure"));
					}else if(diseaseName.equals("Diabetes")){
						obar.put(getStaticJsonObject("diabetes"));
					}else if(diseaseName.equals("Tuberculosis")){
						obar.put(getStaticJsonObject("tuberculosis"));
					}else if(diseaseName.equals("Others_member_disease")){
						obar.put(getStaticJsonObject("otherPossibleDisease"));
					}
				}
			}
		}else{
			obar.put(getStaticJsonObject("healthCareGivenNo"));
		}
		
		obar = addRefferedPlaceInObservationArray(e, obar, formFieldPath);
		return obar;
	}
	
	private JSONArray addRefferedPlaceInObservationArray(Event e, JSONArray obar, String formFieldPath) throws JSONException{
		List<Obs> eventObs = e.getObs();
		if(eventObs!= null){
			for(Obs o: eventObs){
				String formSubmissionField = o.getFormSubmissionField();
				if(formSubmissionField!= null){
					String obsValue = (String) o.getValues().get(0);
					if(formSubmissionField.equals("Place_of_Refer") && obsValue!= null){
						if(!formFieldPath.isEmpty()){
							JSONObject refferedPlace = getStaticJsonObject(obsValue);
							refferedPlace.put("formFieldPath", formFieldPath);
							obar.put(refferedPlace);
						}else{
							obar.put(getStaticJsonObject(obsValue));
						}
					}
				}
			}
		}
		return obar;
	}
	
// not needed if we put static json object in observation : February 4, 2019
/*	public JSONObject setCommonObservationInfo(HashMap<String, Object> commonValuesMap) throws JSONException {
		JSONObject observationObject = new JSONObject();
		JSONObject conceptObject = new JSONObject();
		String uuid = (String) commonValuesMap.get("concept_uuid");
		String name = (String) commonValuesMap.get("concept_name");
		conceptObject.put("uuid", uuid);
		conceptObject.put("name", name);
		observationObject.put("concept", conceptObject);

		String formNamespace = (String) commonValuesMap.get("formNamespace");
		observationObject.put("formNamespace", formNamespace);
		String formFieldPath = (String) commonValuesMap.get("formFieldPath");
		observationObject.put("formFieldPath", formFieldPath);
		boolean voided = (boolean) commonValuesMap.get("voided");
		observationObject.put("voided", voided);
		Object interpretation = commonValuesMap.get("interpretation");
		observationObject.put("interpretation", interpretation);
		JSONArray groupMembers = (JSONArray)commonValuesMap.get("groupMembers");
		observationObject.put("groupMembers", groupMembers);
		boolean inactive = (boolean) commonValuesMap.get("inactive");
		observationObject.put("inactive", inactive);
		return observationObject;
	}*/
	
	public JSONObject buildUpdateEncounter(Event e) throws JSONException {
		String openmrsuuid = e.getIdentifier(OPENMRS_UUID_IDENTIFIER_TYPE);
		JSONObject encounterObsUuids = getObsByEncounterUuid(openmrsuuid);
		JSONArray obsUuids = encounterObsUuids.getJSONArray("obs");
		
		System.out.print("[OBS-UUIDS]" + obsUuids);
		
		JSONObject pt = patientService.getPatientByIdentifier(e.getBaseEntityId());//TODO find by any identifier
		JSONObject enc = new JSONObject();
		
		JSONObject pr = userService.getPersonByUser(e.getProviderId());
		
		enc.put("encounterDatetime", OPENMRS_DATE.format(e.getEventDate().toDate()));
		// patient must be existing in OpenMRS before it submits an encounter. if it doesnot it would throw NPE
		enc.put("patient", pt.getString("uuid"));
		//TODO	enc.put("patientUuid", pt.getString("uuid"));
		enc.put("encounterType", e.getEventType());
		enc.put("location", e.getLocationId());
		//enc.put("provider", pr.has("uuid") ? pr.getString("uuid") : "");
		
		List<Obs> ol = e.getObs();
		Map<String, JSONArray> p = new HashMap<>();
		Map<String, JSONArray> pc = new HashMap<>();
		
		if (ol != null)
			for (Obs obs : ol) {
				if (!StringUtils.isEmptyOrWhitespaceOnly(obs.getFieldCode())
				        && (obs.getFieldType() == null || obs.getFieldType().equalsIgnoreCase("concept"))) {//skipping empty obs
					//if no parent simply make it root obs
					if (StringUtils.isEmptyOrWhitespaceOnly(obs.getParentCode())) {
						p.put(obs.getFieldCode(), convertObsToJson(obs));
					} else {
						//find parent obs if not found search and fill or create one
						JSONArray parentObs = p.get(obs.getParentCode());
						if (parentObs == null) {
							p.put(obs.getParentCode(), convertObsToJson(getOrCreateParent(ol, obs)));
						}
						// find if any other exists with same parent if so add to the list otherwise create new list
						JSONArray obl = pc.get(obs.getParentCode());
						if (obl == null) {
							obl = new JSONArray();
						}
						JSONArray addobs = convertObsToJson(obs);
						for (int i = 0; i < addobs.length(); i++) {
							obl.put(addobs.getJSONObject(i));
						}
						pc.put(obs.getParentCode(), obl);
					}
				}
			}
		
		JSONArray obar = new JSONArray();
		for (String ok : p.keySet()) {
			for (int i = 0; i < p.get(ok).length(); i++) {
				JSONObject obo = p.get(ok).getJSONObject(i);
				obo.put("uuid", getObsUuid(obo, obsUuids));
				
				JSONArray cob = pc.get(ok);
				if (cob != null && cob.length() > 0) {
					// Fetch children obs uuids
					JSONObject obsGroupUuids = getObsUuidByParentObsUuid(obo.getString("uuid"));
					JSONArray groupUuids = obsGroupUuids.getJSONArray("groupMembers");
					// Add uuids to group members
					for (int j = 0; j < cob.length(); j++) {
						JSONObject cobObj = cob.getJSONObject(j);
						cobObj.put("uuid", getObsUuid(cobObj, groupUuids));
					}
					
					obo.put("groupMembers", cob);
				}
				
				obar.put(obo);
			}
		}
		//enc.put("obs", obar);
		
		return enc;
	}
	
	public JSONObject updateEncounter(Event e) throws JSONException {
		if (StringUtils.isEmptyOrWhitespaceOnly(e.getIdentifier(OPENMRS_UUID_IDENTIFIER_TYPE))) {
			throw new IllegalArgumentException("Encounter was never pushed to OpenMRS as " + OPENMRS_UUID_IDENTIFIER_TYPE
			        + " is empty. Consider creating a new one");
		}
		
		String openmrsuuid = e.getIdentifier(OPENMRS_UUID_IDENTIFIER_TYPE);
		
		JSONObject enc = buildUpdateEncounter(e);
		
		HttpResponse op = HttpUtil.post(HttpUtil.removeEndingSlash(OPENMRS_BASE_URL) + "/" + ENCOUNTER_URL + "/"
		        + openmrsuuid, "", enc.toString(), OPENMRS_USER, OPENMRS_PWD);
		return new JSONObject(op.body());
	}
	
	private String getObsUuid(JSONObject obs, JSONArray obsUuids) throws JSONException {
		String uuid = "";
		// obs = {"concept":"163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"}
		// obsUuids = [{"concept":{"uuid":"163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"},"uuid":"b267b2f5-94be-43e8-85c4-4e36f2eb8471"}, {}]
		
		for (int i = 0; i < obsUuids.length(); i++) {
			JSONObject obsUuid = obsUuids.getJSONObject(i);
			JSONObject conceptObj = obsUuid.getJSONObject("concept");
			
			if (conceptObj.get("uuid").equals(obs.get("concept"))) {
				return obsUuid.getString("uuid");
			}
		}
		
		return uuid;
	}
	
	private JSONArray convertObsToJson(Obs o) throws JSONException {
		JSONArray arr = new JSONArray();
		if (o.getValues() == null || o.getValues().size() == 0) {//must be parent of some obs
			JSONObject obo = new JSONObject();
			obo.put("concept", o.getFieldCode());
			
			arr.put(obo);
		} else {
			//OpenMRS can not handle multivalued obs so add obs with multiple values as two different obs
			for (Object v : o.getValues()) {
				JSONObject obo = new JSONObject();
				obo.put("concept", o.getFieldCode());
				obo.put("value", v);
				
				arr.put(obo);
			}
		}
		return arr;
	}
	
	private Obs getOrCreateParent(List<Obs> obl, Obs o) {
		for (Obs obs : obl) {
			if (o.getParentCode().equalsIgnoreCase(obs.getFieldCode())) {
				return obs;
			}
		}
		return new Obs("concept", "parent", o.getParentCode(), null, null, null, null);
	}
	
	// TODO needs review and refactor
	public Event makeNewEventForNewClient(Client c, String eventType, String entityType) {
		Event event = new Event();
		try {
			String locationId = "";
			String ward = c.getAddresses().get(0).getAddressField("address2");
			org.opensrp.api.domain.Location location = null;
			location = openmrsLocationService.getLocation(ward);
			locationId = location.getLocationId();
			
			event.setServerVersion(System.currentTimeMillis());
			event.setTeam("");
			event.setTeamId("");
			event.setBaseEntityId(c.getBaseEntityId());
			event.setDateCreated(new DateTime());
			event.setEventDate(new DateTime());
			event.withProviderId("");
			event.setVersion(System.currentTimeMillis());
			event.setLocationId(locationId);
			event.setFormSubmissionId(UUID.randomUUID().toString().trim());
			event.withIsSendToOpenMRS("no").withEventType(eventType).withEntityType(entityType);
			List<String> eventAddress = new ArrayList<String>();
			eventAddress.add("BANGLADESH");
			eventAddress.add(c.getAddresses().get(0).getAddressField("stateProvince"));
			eventAddress.add(c.getAddresses().get(0).getAddressField("countyDistrict"));
			eventAddress.add(c.getAddresses().get(0).getAddressField("cityVillage"));
			eventAddress.add(c.getAddresses().get(0).getAddressField("address1"));
			eventAddress.add(c.getAddresses().get(0).getAddressField("address2"));
			JSONArray addressFieldValue = new JSONArray(eventAddress);
			event.addObs(new Obs("formsubmissionField", "text", "HIE_FACILITIES", "" /*//TODO handle parent*/,
			        addressFieldValue.toString(), ""/*comments*/, "HIE_FACILITIES"/*formSubmissionField*/));
			
			eventService.addorUpdateEvent(event);
		}
		catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return event;
		
	}
	
	public Event convertToEvent(JSONObject encounter) throws JSONException {
		if (encounter.has("patientUuid") == false) {
			throw new IllegalStateException("No 'patient' object found in given encounter");
		}
		Event e = new Event();
		String patientId = encounter.getString("patientId");
		String patientUuid = encounter.getString("patientUuid");
		Client c = clientService.find(patientId);
		if (c == null || c.getBaseEntityId() == null) {
			//try to get the client from openmrs based on the uuid
			JSONObject openmrsPatient = patientService.getPatientByUuid(patientUuid, false);
			c = patientService.convertToClient(openmrsPatient);
			if (c == null || c.getBaseEntityId() == null) {
				throw new IllegalStateException(
				        "Client was not found registered while converting Encounter to an Event in OpenSRP");
			} else {
				//clientService.addClient(c);// currently not valid
			}
		}
		List<Event> events = eventService.findByBaseEntityId(c.getBaseEntityId());
		String providerId = "";
		if (events.size() != 0) {
			providerId = events.get(0).getProviderId();
		}
		//JSONObject creator = encounter.getJSONObject("auditInfo").getJSONObject("creator");
		e.withBaseEntityId(c.getBaseEntityId())
		//.withCreator(new User(creator.getString("uuid"), creator.getString("display"), null, null))
		        .withDateCreated(DateTime.now());
		
		e.withEventDate(new DateTime(encounter.getString("encounterDateTime")))
		        //.withEntityType(entityType) //TODO
		        .withEventType(encounter.getString("encounterType"))
		        .withFormSubmissionId(encounter.getString("encounterUuid"))//TODO
		        .withLocationId(encounter.getString("locationUuid"))
		        //TODO manage providers and uuid in couch
		        .withProviderId(providerId);
		
		e.addIdentifier(OPENMRS_UUID_IDENTIFIER_TYPE, encounter.getString("encounterUuid"));
		
		JSONArray ol = encounter.getJSONArray("observations");
		for (int i = 0; i < ol.length(); i++) {
			JSONObject o = ol.getJSONObject(i);
			List<Object> values = new ArrayList<Object>();
			List<Object> humanReadableValues = new ArrayList<Object>();
			if (o.optJSONObject("value") != null) {
				values.add(o.getString("valueAsString"));
				humanReadableValues.add(o.getJSONObject("value").getString("name"));
			} else if (o.has("value")) {
				values.add(o.getString("value"));
				humanReadableValues.add(o.getString("value"));
			}
			String fieldDataType = o.getJSONObject("concept").getString("dataType");
			if ("N/A".equalsIgnoreCase(fieldDataType)) {
				fieldDataType = "text";
			}
			
			e.addObs(new Obs("concept", fieldDataType, o.getJSONObject("concept").getString("uuid"),
			        "" /*//TODO handle parent*/, values, humanReadableValues, ""/*comments*/, o.getJSONObject("concept")
			                .getString("shortName")/*formSubmissionField*/));
		}
		
		return e;
	}
	
	
	public JSONObject getStaticJsonObject(String nameOfJSONObject) {
		JSONObject normalDisease = null;
		JSONObject objectToReturn = null;
		JSONObject diabetes = null;
		JSONObject highBloodPressure = null;
		JSONObject healthCareGivenYes = null;
		JSONObject healthCareGivenNo = null;
		JSONObject tuberculosis = null;
		JSONObject otherPossibleDisease = null;
		JSONObject unionSubCenter = null;
		JSONObject unionFamilyWelfareCenter = null;
		JSONObject unionHealthAndFamilyWelfareCenter = null;
		JSONObject metarnalAndChildWelfareCenter = null;
		JSONObject tenBedHospital = null;
		JSONObject twentyBedHospital = null;
		JSONObject upazilaHealthComplex = null;
		JSONObject districtHospital = null;
		JSONObject medicalCollegeAndHospital = null;
		JSONObject otherHealthFacility = null;
		
		JSONObject familyPlanningCHCP = null;
		JSONObject oralContraceptives = null;
		JSONObject condoms = null;
		JSONObject injectable = null;
		JSONObject otherMethod = null;
		
		JSONObject haveDangerSignsPregnancyYes = null;
		JSONObject bleedingThroughBirthCanal = null;
		JSONObject prolongedDelivery = null;
		JSONObject edema = null;
		JSONObject jaundice = null;
		JSONObject convulsion = null;
		JSONObject highTemperature = null;
		JSONObject weaknessBlurredVision = null;
		
		JSONObject verySevereDisease = null;
		JSONObject probableLimitedInfection = null;
		JSONObject bellyButtonInfection = null;
		JSONObject injury = null;
		JSONObject fever = null;
		JSONObject pneumonia = null;
		JSONObject coldAndCough = null;
		JSONObject diarrhoeaNoDehydration = null;
		JSONObject othersMemberDisease = null;
		
		JSONObject diarrhoeaAndDysentery = null;
		JSONObject maleria = null;
		JSONObject hearingLoss = null;
		JSONObject measles = null;
		JSONObject conjunctivitis = null;
		JSONObject malnutrition = null;
		JSONObject anemia = null;
		try {
			//normalDisease = new JSONObject("{\"encounterTypeUuid\":\"81852aee-3f10-11e4-adec-0800271c1b75\",\"visitType\":\"Community clinic service\",\"patientUuid\":\"391ec594-5381-4075-9b1d-7608ed19332d\",\"locationUuid\":\"ec9bfa0e-14f2-440d-bf22-606605d021b2\",\"providers\":[{\"uuid\":\"313c8507-9821-40e4-8a70-71a5c7693d72\"}]}");
			normalDisease = new JSONObject("{\"encounterTypeUuid\":\"81852aee-3f10-11e4-adec-0800271c1b75\",\"providers\":[{\"uuid\":\"313c8507-9821-40e4-8a70-71a5c7693d72\"}],\"visitType\":\"Community clinic service\"}");
			diabetes = new JSONObject("{\"concept\":{\"uuid\":\"a725f0d7-067b-492d-a450-4ce7e535c371\",\"name\":\"Possible_Disease\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"সাধারন রোগীর সেবা.19/31-0\",\"voided\":false,\"value\":{\"uuid\":\"1e3f1870-b252-4808-8edb-f86fad050ebd\",\"name\":{\"display\":\"Diabetes\",\"uuid\":\"befce65b-9e80-45ec-b8b7-05234cd5cb9c\",\"name\":\"Diabetes\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Diabetes\",\"resourceVersion\":\"2.0\",\"translationKey\":\"ডায়াবেটিস_31\"},\"inactive\":false,\"groupMembers\":[]}");
			healthCareGivenYes = new JSONObject("{\"concept\":{\"uuid\":\"f2671938-ffc5-4547-91c0-fcd28b6e29b4\",\"name\":\"Provide_Health_Service\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"সাধারন রোগীর সেবা.19/43-0\",\"voided\":false,\"value\":{\"uuid\":\"a2065636-5326-40f5-aed6-0cc2cca81ccc\",\"name\":{\"display\":\"Yes\",\"uuid\":\"b5a4d83a-7158-4477-b81c-71144f5a7232\",\"name\":\"Yes\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Yes\",\"resourceVersion\":\"2.0\",\"translationKey\":\"হ্যাঁ_43\"},\"interpretation\":null,\"inactive\":false,\"groupMembers\":[]}");
			highBloodPressure = new JSONObject("{\"concept\":{\"uuid\":\"a725f0d7-067b-492d-a450-4ce7e535c371\",\"name\":\"Possible_Disease\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"সাধারন রোগীর সেবা.19/31-0\",\"voided\":false,\"value\":{\"uuid\":\"c2bb6edf-18cb-4c7f-ad91-7c8dd561a437\",\"name\":{\"display\":\"High Blood Pressure\",\"uuid\":\"c2bb6edf-18cb-4c7f-ad91-7c8dd561a437\",\"name\":\"High Blood Pressure\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"High Blood Pressure\",\"resourceVersion\":\"2.0\",\"translationKey\":\"উচ্চ_রক্তচাপ_31\"},\"inactive\":false,\"groupMembers\":[]}");
			healthCareGivenNo = new JSONObject("{\"groupMembers\":[],\"inactive\":false,\"interpretation\":null,\"concept\":{\"name\":\"Provide_Health_Service\",\"uuid\":\"f2671938-ffc5-4547-91c0-fcd28b6e29b4\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"সাধারন রোগীর সেবা.19/43-0\",\"voided\":false,\"value\":{\"translationKey\":\"না_43\",\"displayString\":\"No\",\"resourceVersion\":\"2.0\",\"name\":{\"display\":\"No\",\"resourceVersion\":\"1.9\",\"name\":\"No\",\"localePreferred\":true,\"locale\":\"en\",\"uuid\":\"17432139-eeca-4cf5-b0fd-00a6a4f83395\",\"conceptNameType\":null},\"uuid\":\"b497171e-0410-4d8d-bbd4-7e1a8f8b504e\"}}");
			tuberculosis = new JSONObject("{\"groupMembers\":[],\"inactive\":false,\"concept\":{\"name\":\"Possible_Disease\",\"uuid\":\"a725f0d7-067b-492d-a450-4ce7e535c371\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"সাধারন রোগীর সেবা.19/31-0\",\"voided\":false,\"value\":{\"translationKey\":\"যক্ষ্মা_31\",\"displayString\":\"Tuberculosis\",\"resourceVersion\":\"2.0\",\"name\":{\"display\":\"Tuberculosis\",\"resourceVersion\":\"1.9\",\"name\":\"Tuberculosis\",\"localePreferred\":true,\"locale\":\"en\",\"uuid\":\"d1183ae6-825f-478b-abd7-225d2a234da5\",\"conceptNameType\":null},\"uuid\":\"0622f52f-0c95-41c1-ab5d-ee9bc335c839\"}}");
			otherPossibleDisease = new JSONObject("{\"concept\":{\"uuid\":\"a725f0d7-067b-492d-a450-4ce7e535c371\",\"name\":\"Possible_Disease\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"সাধারন রোগীর সেবা.19/31-0\",\"voided\":false,\"value\":{\"uuid\":\"2531ef53-76fe-4f71-b5ce-675701a3e02a\",\"name\":{\"display\":\"Other_Possible_Diseases\",\"uuid\":\"d838f73b-5bd9-43bd-accd-974da1efc1f2\",\"name\":\"Other_Possible_Diseases\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Other_Possible_Diseases\",\"resourceVersion\":\"2.0\",\"translationKey\":\"অন্যান্য_সম্ভাব্য_রোগ_31\"},\"inactive\":false,\"groupMembers\":[]}");
			unionSubCenter = new JSONObject("{\"concept\":{\"uuid\":\"953bc1ec-ca20-4db1-8de2-48feb51377e3\",\"name\":\"CHCP_PLACE_OF_REFER\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"সাধারন রোগীর সেবা.19/47-0\",\"voided\":false,\"value\":{\"uuid\":\"094fcced-08c3-484f-9260-00f9f852d695\",\"name\":{\"display\":\"Union_Sub_Center\",\"uuid\":\"0ce085be-4e2e-4b55-884d-a438157a2d10\",\"name\":\"Union_Sub_Center\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Union_Sub_Center\",\"resourceVersion\":\"2.0\",\"translationKey\":\"ইউনিয়ন_উপস্বাস্থ্য_কেন্দ্র_47\"},\"inactive\":false,\"groupMembers\":[],\"interpretation\":null}");
			unionFamilyWelfareCenter = new JSONObject("{\"concept\":{\"uuid\":\"953bc1ec-ca20-4db1-8de2-48feb51377e3\",\"name\":\"CHCP_PLACE_OF_REFER\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"সাধারন রোগীর সেবা.19/47-0\",\"voided\":false,\"value\":{\"uuid\":\"729aa7bb-4270-4e1f-bb37-8dc4acedae70\",\"name\":{\"display\":\"Union_Family_Welfare_Center\",\"uuid\":\"a2db6720-0e59-42c1-821a-16df38077a2c\",\"name\":\"Union_Family_Welfare_Center\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Union_Family_Welfare_Center\",\"resourceVersion\":\"2.0\",\"translationKey\":\"ইউনিয়ন_পরিবার_কল্যাণ_কেন্দ্র_47\"},\"inactive\":false,\"groupMembers\":[],\"interpretation\":null}");
			unionHealthAndFamilyWelfareCenter = new JSONObject("{\"concept\":{\"uuid\":\"953bc1ec-ca20-4db1-8de2-48feb51377e3\",\"name\":\"CHCP_PLACE_OF_REFER\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"সাধারন রোগীর সেবা.19/47-0\",\"voided\":false,\"value\":{\"uuid\":\"2b4e02e2-11b2-48e4-b218-8adca3dc1731\",\"name\":{\"display\":\"Union_Health_and_Family_Welfare_Center\",\"uuid\":\"09740f60-6238-4b29-a917-3b38dc03a129\",\"name\":\"Union_Health_and_Family_Welfare_Center\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Union_Health_and_Family_Welfare_Center\",\"resourceVersion\":\"2.0\",\"translationKey\":\"ইউনিয়ন_স্বাস্থ্য_ও_পরিবার_কল্যাণ_কেন্দ্র_47\"},\"interpretation\":null,\"inactive\":false,\"groupMembers\":[]}");
			metarnalAndChildWelfareCenter = new JSONObject("{\"groupMembers\":[],\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"সাধারন রোগীর সেবা.19/47-0\",\"interpretation\":null,\"voided\":false,\"concept\":{\"uuid\":\"953bc1ec-ca20-4db1-8de2-48feb51377e3\",\"name\":\"CHCP_PLACE_OF_REFER\"},\"value\":{\"uuid\":\"ff45d730-5c44-45e8-a869-64e4cdf2f2ca\",\"name\":{\"display\":\"Metarnal_and_Child_Wellfare_Center\",\"uuid\":\"456e953e-1b94-442d-943a-b5dfc4b3cb60\",\"name\":\"Metarnal_and_Child_Wellfare_Center\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Metarnal_and_Child_Wellfare_Center\",\"resourceVersion\":\"2.0\",\"translationKey\":\"মা_ও_শিশু_কল্যাণ_কেন্দ্র_47\"},\"inactive\":false}");
			tenBedHospital = new JSONObject("{\"groupMembers\":[],\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"সাধারন রোগীর সেবা.19/47-0\",\"interpretation\":null,\"voided\":false,\"concept\":{\"uuid\":\"953bc1ec-ca20-4db1-8de2-48feb51377e3\",\"name\":\"CHCP_PLACE_OF_REFER\"},\"value\":{\"uuid\":\"7a34aa8e-f6f7-4abc-ad62-79bae8386155\",\"name\":{\"display\":\"10_Bed_Hospital\",\"uuid\":\"346f4ebd-9403-49cd-a301-e7f3bff92853\",\"name\":\"10_Bed_Hospital\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"10_Bed_Hospital\",\"resourceVersion\":\"2.0\",\"translationKey\":\"১০_শয্যা_বিশিষ্ট_হাসপাতাল_47\"},\"inactive\":false}");
			twentyBedHospital = new JSONObject("{\"concept\":{\"uuid\":\"953bc1ec-ca20-4db1-8de2-48feb51377e3\",\"name\":\"CHCP_PLACE_OF_REFER\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"সাধারন রোগীর সেবা.19/47-0\",\"voided\":false,\"value\":{\"uuid\":\"8be604e8-ca58-4bdb-b611-07cd3c553428\",\"name\":{\"display\":\"20_Beds_Hospital\",\"uuid\":\"3de85581-fcac-4c85-9dbc-782e637490f6\",\"name\":\"20_Beds_Hospital\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"20_Beds_Hospital\",\"resourceVersion\":\"2.0\",\"translationKey\":\"২০_শয্যা_বিশিষ্ট_হাসপাতাল_47\"},\"interpretation\":null,\"inactive\":false,\"groupMembers\":[]}");
			upazilaHealthComplex = new JSONObject("{\"concept\":{\"uuid\":\"953bc1ec-ca20-4db1-8de2-48feb51377e3\",\"name\":\"CHCP_PLACE_OF_REFER\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"সাধারন রোগীর সেবা.19/47-0\",\"voided\":false,\"value\":{\"uuid\":\"8f6e53ef-f23a-41d3-8474-0d654d453068\",\"name\":{\"display\":\"Upazila_Health_Complex\",\"uuid\":\"35cc74c1-707e-45b6-b1b4-379e1ab8bd25\",\"name\":\"Upazila_Health_Complex\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Upazila_Health_Complex\",\"resourceVersion\":\"2.0\",\"translationKey\":\"উপজেলা_স্বাস্থ্য_কমপ্লেক্স_47\"},\"interpretation\":null,\"inactive\":false,\"groupMembers\":[]}");
			districtHospital = new JSONObject("{\"concept\":{\"uuid\":\"953bc1ec-ca20-4db1-8de2-48feb51377e3\",\"name\":\"CHCP_PLACE_OF_REFER\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"সাধারন রোগীর সেবা.19/47-0\",\"voided\":false,\"value\":{\"uuid\":\"077bbfb9-a7b6-485c-9d8d-12cf32eaf47c\",\"name\":{\"display\":\"District_Hospital\",\"uuid\":\"30018807-1c8d-46a8-8978-1377a76e7fc5\",\"name\":\"District_Hospital\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"District_Hospital\",\"resourceVersion\":\"2.0\",\"translationKey\":\"সদর_হাসপাতাল_47\"},\"interpretation\":null,\"inactive\":false,\"groupMembers\":[]}");
			medicalCollegeAndHospital = new JSONObject("{\"concept\":{\"uuid\":\"953bc1ec-ca20-4db1-8de2-48feb51377e3\",\"name\":\"CHCP_PLACE_OF_REFER\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"সাধারন রোগীর সেবা.19/47-0\",\"voided\":false,\"value\":{\"uuid\":\"cdb1918b-08aa-4d27-829f-44759e1b8a24\",\"name\":{\"display\":\"Medical_College_and_Hospital\",\"uuid\":\"cd22cdf6-cceb-48fb-b079-9f0e840b5e1f\",\"name\":\"Medical_College_and_Hospital\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Medical_College_and_Hospital\",\"resourceVersion\":\"2.0\",\"translationKey\":\"মেডিকেল_কলেজ_হাসপাতাল_47\"},\"interpretation\":null,\"inactive\":false,\"groupMembers\":[]}");
			otherHealthFacility = new JSONObject("{\"concept\":{\"uuid\":\"953bc1ec-ca20-4db1-8de2-48feb51377e3\",\"name\":\"CHCP_PLACE_OF_REFER\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"সাধারন রোগীর সেবা.19/47-0\",\"voided\":false,\"value\":{\"uuid\":\"41bbac3f-5164-4dac-a2ec-8648bf8a7d89\",\"name\":{\"display\":\"Others_Health_Facility\",\"uuid\":\"fe8c216a-fa59-499f-aa8d-22c1a724506e\",\"name\":\"Others_Health_Facility\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Others_Health_Facility\",\"resourceVersion\":\"2.0\",\"translationKey\":\"অন্যান্য_স্বাস্থ্য_সেবা_কেন্দ্র_47\"},\"interpretation\":null,\"inactive\":false,\"groupMembers\":[]}");
		
			familyPlanningCHCP = new JSONObject("{\"concept\":{\"uuid\":\"5265ff17-2936-4be3-af95-f817a0c5e4b1\",\"name\":\"FAMILY_PLANNING_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"পরিবার পরিকল্পনা সেবা.2/6-0\",\"voided\":false,\"inactive\":false}");
			oralContraceptives = new JSONObject("{\"concept\":{\"uuid\":\"a7526490-7b21-44ec-8174-bcb4647703ca\",\"name\":\"FAMILY_PLANNING_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"পরিবার পরিকল্পনা সেবা.2/7-0\",\"voided\":false,\"groupMembers\":[],\"inactive\":false,\"value\":{\"uuid\":\"9b76de10-cbee-4b8a-901e-81e39936dd7e\",\"name\":{\"display\":\"Oral Contraceptives\",\"uuid\":\"21e0f743-08fe-4a4d-b1c9-708dea051933\",\"name\":\"Oral Contraceptives\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Oral Contraceptives\",\"resourceVersion\":\"2.0\",\"translationKey\":\"খাবার_বড়ি_7\"},\"interpretation\":null}");
			condoms = new JSONObject("{\"concept\":{\"uuid\":\"a7526490-7b21-44ec-8174-bcb4647703ca\",\"name\":\"FAMILY_PLANNING_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"পরিবার পরিকল্পনা সেবা.2/7-0\",\"voided\":false,\"groupMembers\":[],\"inactive\":false,\"value\":{\"uuid\":\"1fe0597e-470d-49bd-9d82-9c7b7342dab0\",\"name\":{\"display\":\"Condoms\",\"uuid\":\"d4300218-a8fa-4ca4-b0a3-5b8cef1a4249\",\"name\":\"Condoms\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Condoms\",\"resourceVersion\":\"2.0\",\"translationKey\":\"কনডম_7\"},\"interpretation\":null}");
			injectable = new JSONObject("{\"concept\":{\"uuid\":\"a7526490-7b21-44ec-8174-bcb4647703ca\",\"name\":\"FAMILY_PLANNING_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"পরিবার পরিকল্পনা সেবা.2/7-0\",\"voided\":false,\"value\":{\"uuid\":\"f80264f6-ba9d-4b8c-a15a-9076bef6ac8a\",\"name\":{\"display\":\"Injectable\",\"uuid\":\"c5e9ff44-06d8-4d69-8cf4-9b51aee71fdd\",\"name\":\"Injectable\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Injectable\",\"resourceVersion\":\"2.0\",\"translationKey\":\"ইনজেক্টবল_7\"},\"interpretation\":null,\"inactive\":false,\"groupMembers\":[]}");
			otherMethod = new JSONObject("{\"concept\":{\"uuid\":\"a7526490-7b21-44ec-8174-bcb4647703ca\",\"name\":\"FAMILY_PLANNING_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"পরিবার পরিকল্পনা সেবা.2/7-0\",\"voided\":false,\"value\":{\"uuid\":\"4fdc5b5b-ff7a-4bdf-920f-92276ef6c07f\",\"name\":{\"display\":\"Other_Method\",\"uuid\":\"4774fe09-e957-4c53-955a-a2b43ee9fe98\",\"name\":\"Other_Method\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Other_Method\",\"resourceVersion\":\"2.0\",\"translationKey\":\"অন্যান্য_পদ্ধতি_7\"},\"interpretation\":null,\"inactive\":false,\"groupMembers\":[]}");
		
			haveDangerSignsPregnancyYes = new JSONObject("{\"concept\":{\"uuid\":\"519c7a61-b3bc-45db-a437-897c640c7c62\",\"name\":\"Have_Danger_Signs_Pregnancy\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"প্রসব পূর্ব সেবা.86/112-0\",\"voided\":false,\"value\":{\"uuid\":\"a2065636-5326-40f5-aed6-0cc2cca81ccc\",\"name\":{\"display\":\"Yes\",\"uuid\":\"b5a4d83a-7158-4477-b81c-71144f5a7232\",\"name\":\"Yes\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Yes\",\"resourceVersion\":\"2.0\",\"translationKey\":\"হ্যাঁ_112\"},\"interpretation\":null,\"inactive\":false,\"groupMembers\":[]}");
			bleedingThroughBirthCanal = new JSONObject("{\"concept\":{\"uuid\":\"d84040fb-d3b6-40fa-b292-a26f90079464\",\"name\":\"Have_Danger_Signs_Pregnancy\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"প্রসব পূর্ব সেবা.86/78-0\",\"voided\":false,\"value\":{\"uuid\":\"3cdc7795-8305-4d43-a279-d9a1bb8f04a7\",\"name\":{\"display\":\"Bleeding_Through_Birth_Canal\",\"uuid\":\"07b2678c-dcf2-4c4d-90b1-cd22cdaeeac3\",\"name\":\"Bleeding_Through_Birth_Canal\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Bleeding_Through_Birth_Canal\",\"resourceVersion\":\"2.0\",\"translationKey\":\"যোনিপথে_রক্তক্ষরণ_78\"},\"inactive\":false,\"groupMembers\":[]}");
			prolongedDelivery = new JSONObject("{\"concept\":{\"uuid\":\"d84040fb-d3b6-40fa-b292-a26f90079464\",\"name\":\"Have_Danger_Signs_Pregnancy\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"প্রসব পূর্ব সেবা.86/78-0\",\"voided\":false,\"value\":{\"uuid\":\"09d1e5f0-86f3-4a69-a137-2e941d31883c\",\"name\":{\"display\":\"Prolonged_Delivery_or_Child_Coming_Out_Before\",\"uuid\":\"6d2de0be-0551-48e2-85bc-a20637995019\",\"name\":\"Prolonged_Delivery_or_Child_Coming_Out_Before\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Prolonged_Delivery_or_Child_Coming_Out_Before\",\"resourceVersion\":\"2.0\",\"translationKey\":\"প্রলম্বিত_প্রসব/_বাচ্চা_আগে_বের_হওয়া_78\"},\"inactive\":false,\"groupMembers\":[]}");
			edema = new JSONObject("{\"concept\":{\"uuid\":\"d84040fb-d3b6-40fa-b292-a26f90079464\",\"name\":\"Have_Danger_Signs_Pregnancy\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"প্রসব পূর্ব সেবা.86/78-0\",\"voided\":false,\"value\":{\"uuid\":\"5da2db65-3dc0-4ad8-8ce3-b15f9cef3bc0\",\"name\":{\"display\":\"Edema\",\"uuid\":\"08d59498-27be-40b1-8478-f87b65bbf5bf\",\"name\":\"Edema\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Edema\",\"resourceVersion\":\"2.0\",\"translationKey\":\"ইডেমা_78\"},\"inactive\":false,\"groupMembers\":[]}");
			jaundice = new JSONObject("{\"concept\":{\"uuid\":\"d84040fb-d3b6-40fa-b292-a26f90079464\",\"name\":\"Have_Danger_Signs_Pregnancy\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"প্রসব পূর্ব সেবা.86/78-0\",\"voided\":false,\"value\":{\"uuid\":\"f20b15b2-4e14-11e4-8a57-0800271c1b75\",\"name\":{\"display\":\"Jaundice\",\"uuid\":\"40b3fb68-6ddd-4e5f-a94e-5dd758190a50\",\"name\":\"Jaundice\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Jaundice\",\"resourceVersion\":\"2.0\",\"translationKey\":\"জন্ডিস_78\"},\"inactive\":false,\"groupMembers\":[]}");
			convulsion = new JSONObject("{\"concept\":{\"uuid\":\"d84040fb-d3b6-40fa-b292-a26f90079464\",\"name\":\"Have_Danger_Signs_Pregnancy\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"প্রসব পূর্ব সেবা.86/78-0\",\"voided\":false,\"value\":{\"uuid\":\"f1806ea3-da0b-4442-827a-b85f26f038db\",\"name\":{\"display\":\"Convulsion\",\"uuid\":\"5fab5283-1e6b-4653-95bf-cbbae8f4f8d3\",\"name\":\"Convulsion\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Convulsion\",\"resourceVersion\":\"2.0\",\"translationKey\":\"খিঁচুনি_78\"},\"inactive\":false,\"groupMembers\":[]}");
			highTemperature = new JSONObject("{\"concept\":{\"uuid\":\"d84040fb-d3b6-40fa-b292-a26f90079464\",\"name\":\"Have_Danger_Signs_Pregnancy\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"প্রসব পূর্ব সেবা.86/78-0\",\"voided\":false,\"value\":{\"uuid\":\"86c06eec-beee-4d0e-9d16-db57139dd857\",\"name\":{\"display\":\"High_Temperature_102_Degree_or_More\",\"uuid\":\"694c67b4-ff16-4326-ac25-3c00e561d052\",\"name\":\"High_Temperature_102_Degree_or_More\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"High_Temperature_102_Degree_or_More\",\"resourceVersion\":\"2.0\",\"translationKey\":\"উচ্চ_তাপমাত্রা_১০২_ডিগ্রি_বা_তদুর্ধ_78\"},\"inactive\":false,\"groupMembers\":[]}");
			weaknessBlurredVision = new JSONObject("{\"concept\":{\"uuid\":\"d84040fb-d3b6-40fa-b292-a26f90079464\",\"name\":\"Have_Danger_Signs_Pregnancy\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"প্রসব পূর্ব সেবা.86/78-0\",\"voided\":false,\"value\":{\"uuid\":\"982d4b88-67e1-4fe4-a030-948ad9146847\",\"name\":{\"display\":\"Weakness_Blurred_vision\",\"uuid\":\"a700e629-73a8-435b-9448-929be26e5045\",\"name\":\"Weakness_Blurred_vision\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Weakness_Blurred_vision\",\"resourceVersion\":\"2.0\",\"translationKey\":\"দুর্বলতা,_চোখে_ঝাপসা_দেখা_78\"},\"inactive\":false,\"groupMembers\":[]}");
		
			verySevereDisease = new JSONObject("{\"concept\":{\"uuid\":\"1031ee9f-460c-433d-b0f9-e6aac203d857\",\"name\":\"Disease_Below_2Month_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"শিশু (০ থেকে ২ মাস) স্বাস্থ্য সেবা.35/69-0\",\"voided\":false,\"value\":{\"uuid\":\"8b4bab1a-8ec6-4da8-8725-97a81d7c0ab8\",\"name\":{\"display\":\"Very_severe_disease\",\"uuid\":\"75b3530c-e712-43c8-a846-258b3272f5cf\",\"name\":\"Very_severe_disease\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Very_severe_disease\",\"resourceVersion\":\"2.0\",\"translationKey\":\"খুব_মারাত্বক_রোগ_69\"},\"inactive\":false,\"groupMembers\":[]}");
			probableLimitedInfection = new JSONObject("{\"concept\":{\"uuid\":\"1031ee9f-460c-433d-b0f9-e6aac203d857\",\"name\":\"Disease_Below_2Month_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"শিশু (০ থেকে ২ মাস) স্বাস্থ্য সেবা.35/69-0\",\"voided\":false,\"value\":{\"uuid\":\"f571a834-5caa-49d9-b702-0023999c7808\",\"name\":{\"display\":\"Probable_Limited_Infection\",\"uuid\":\"a6da9b6a-da6c-46b3-a5d8-bf2fe2ff4c9c\",\"name\":\"Probable_Limited_Infection\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Probable_Limited_Infection\",\"resourceVersion\":\"2.0\",\"translationKey\":\"সম্ভাব্য_সীমিত_সংক্রামণ_69\"},\"inactive\":false,\"groupMembers\":[]}");
			bellyButtonInfection = new JSONObject("{\"concept\":{\"uuid\":\"1031ee9f-460c-433d-b0f9-e6aac203d857\",\"name\":\"Disease_Below_2Month_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"শিশু (০ থেকে ২ মাস) স্বাস্থ্য সেবা.35/69-0\",\"voided\":false,\"value\":{\"uuid\":\"dec2d127-e774-41ab-a5dc-cbe7b7d5224a\",\"name\":{\"display\":\"Bellybutton_infection\",\"uuid\":\"dd4a49d2-20f5-49ee-8e6e-be99c25544d2\",\"name\":\"Bellybutton_infection\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Bellybutton_infection\",\"resourceVersion\":\"2.0\",\"translationKey\":\"নাভিতে_সংক্রামন_69\"},\"inactive\":false,\"groupMembers\":[]}");
			injury = new JSONObject("{\"concept\":{\"uuid\":\"1031ee9f-460c-433d-b0f9-e6aac203d857\",\"name\":\"Disease_Below_2Month_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"শিশু (০ থেকে ২ মাস) স্বাস্থ্য সেবা.35/69-0\",\"voided\":false,\"value\":{\"uuid\":\"1faa5af3-4e15-11e4-8a57-0800271c1b75\",\"name\":{\"display\":\"Injury\",\"uuid\":\"12118697-97a1-4033-89a9-9029befddfef\",\"name\":\"Injury\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Injury\",\"resourceVersion\":\"2.0\",\"translationKey\":\"আঘাত_69\"},\"inactive\":false,\"groupMembers\":[]}");
			fever = new JSONObject("{\"concept\":{\"uuid\":\"1031ee9f-460c-433d-b0f9-e6aac203d857\",\"name\":\"Disease_Below_2Month_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"শিশু (০ থেকে ২ মাস) স্বাস্থ্য সেবা.35/69-0\",\"voided\":false,\"value\":{\"uuid\":\"1f0f8ec6-4e15-11e4-8a57-0800271c1b75\",\"name\":{\"display\":\"Fever\",\"uuid\":\"d922012d-78cc-468c-9839-52f7f460f51e\",\"name\":\"Fever\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Fever\",\"resourceVersion\":\"2.0\",\"translationKey\":\"জ্বর_69\"},\"inactive\":false,\"groupMembers\":[]}");
			pneumonia = new JSONObject("{\"concept\":{\"uuid\":\"1031ee9f-460c-433d-b0f9-e6aac203d857\",\"name\":\"Disease_Below_2Month_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"শিশু (০ থেকে ২ মাস) স্বাস্থ্য সেবা.35/69-0\",\"voided\":false,\"value\":{\"uuid\":\"bfe80a20-d10e-4920-8fc2-16870bf7c600\",\"name\":{\"display\":\"Pneumonia\",\"uuid\":\"b1cbdd42-a295-457d-8adb-723e77e45c7d\",\"name\":\"Pneumonia\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Pneumonia\",\"resourceVersion\":\"2.0\",\"translationKey\":\"নিউমোনিয়া_69\"},\"inactive\":false,\"groupMembers\":[]}");
			coldAndCough = new JSONObject("{\"concept\":{\"uuid\":\"1031ee9f-460c-433d-b0f9-e6aac203d857\",\"name\":\"Disease_Below_2Month_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"শিশু (০ থেকে ২ মাস) স্বাস্থ্য সেবা.35/69-0\",\"voided\":false,\"value\":{\"uuid\":\"e6b508fd-4e14-11e4-8a57-0800271c1b75\",\"name\":{\"display\":\"Pneumonia, unspec.\",\"uuid\":\"dfadf888-252c-4231-96e4-ea1b440e2e9c\",\"name\":\"Pneumonia, unspec.\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Pneumonia, unspec.\",\"resourceVersion\":\"2.0\",\"translationKey\":\"কাশি/সর্দি_69\"},\"inactive\":false,\"groupMembers\":[]}");
			diarrhoeaNoDehydration = new JSONObject("{\"concept\":{\"uuid\":\"1031ee9f-460c-433d-b0f9-e6aac203d857\",\"name\":\"Disease_Below_2Month_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"শিশু (০ থেকে ২ মাস) স্বাস্থ্য সেবা.35/69-0\",\"voided\":false,\"value\":{\"uuid\":\"a611cef5-da8f-425d-80e6-cc7025400fba\",\"name\":{\"display\":\"Diarrhoea_No_Dehydration\",\"uuid\":\"3a61f525-81eb-4413-82b5-038f3ed07126\",\"name\":\"Diarrhoea_No_Dehydration\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Diarrhoea_No_Dehydration\",\"resourceVersion\":\"2.0\",\"translationKey\":\"পানি_স্বল্পতাহীন_ডায়রিয়া_69\"},\"inactive\":false,\"groupMembers\":[]}");
			othersMemberDisease = new JSONObject("{\"concept\":{\"uuid\":\"1031ee9f-460c-433d-b0f9-e6aac203d857\",\"name\":\"Disease_Below_2Month_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"শিশু (০ থেকে ২ মাস) স্বাস্থ্য সেবা.35/69-0\",\"voided\":false,\"value\":{\"uuid\":\"af6d9f1e-2e7e-4a61-86ea-f2c001a90781\",\"name\":{\"display\":\"Others_member_disease\",\"uuid\":\"1d074959-b389-4008-9121-f83c4bd9a5ee\",\"name\":\"Others_member_disease\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Others_member_disease\",\"resourceVersion\":\"2.0\",\"translationKey\":\"অন্যান্য_অসুখ_69\"},\"inactive\":false,\"groupMembers\":[]}");
		
			diarrhoeaAndDysentery = new JSONObject("{\"concept\":{\"uuid\":\"ed6dedbf-7bd3-4642-b497-0535e3ee1986\",\"name\":\"Disease_2Months_To_5Years_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"শিশু (২ মাস থেকে ৫ বছর) স্বাস্থ্য সেবা.32/49-0\",\"voided\":false,\"value\":{\"uuid\":\"418c109c-936b-40c8-90ce-58bf6e581371\",\"name\":{\"display\":\"dieriaanddysentry\",\"uuid\":\"5990fd77-f78d-4422-9996-a093278b471a\",\"name\":\"dieriaanddysentry\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"dieriaanddysentry\",\"resourceVersion\":\"2.0\",\"translationKey\":\"ডায়ারিয়া_ও_আমাশয়_49\"},\"inactive\":false,\"groupMembers\":[]}");
			maleria = new JSONObject("{\"concept\":{\"uuid\":\"ed6dedbf-7bd3-4642-b497-0535e3ee1986\",\"name\":\"Disease_2Months_To_5Years_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"শিশু (২ মাস থেকে ৫ বছর) স্বাস্থ্য সেবা.32/49-0\",\"voided\":false,\"value\":{\"uuid\":\"3016d7a5-bb25-4320-97f5-3c724b12c4d7\",\"name\":{\"display\":\"maleria\",\"uuid\":\"ff9f32e2-afa7-4ab6-b531-58f8f316373e\",\"name\":\"maleria\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"maleria\",\"resourceVersion\":\"2.0\",\"translationKey\":\"জ্বর_(ম্যালারিয়া)_49\"},\"inactive\":false,\"groupMembers\":[]}");
			hearingLoss = new JSONObject("{\"concept\":{\"uuid\":\"ed6dedbf-7bd3-4642-b497-0535e3ee1986\",\"name\":\"Disease_2Months_To_5Years_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"শিশু (২ মাস থেকে ৫ বছর) স্বাস্থ্য সেবা.32/49-0\",\"voided\":false,\"value\":{\"uuid\":\"e2062770-4e14-11e4-8a57-0800271c1b75\",\"name\":{\"display\":\"Hearing loss, unspec.\",\"uuid\":\"464bd1cb-0659-418c-a706-87ed72dc6c63\",\"name\":\"Hearing loss, unspec.\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Hearing loss, unspec.\",\"resourceVersion\":\"2.0\",\"translationKey\":\"কানের_সমস্যা_49\"},\"inactive\":false,\"groupMembers\":[]}");
			measles = new JSONObject("{\"concept\":{\"uuid\":\"ed6dedbf-7bd3-4642-b497-0535e3ee1986\",\"name\":\"Disease_2Months_To_5Years_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"শিশু (২ মাস থেকে ৫ বছর) স্বাস্থ্য সেবা.32/49-0\",\"voided\":false,\"value\":{\"uuid\":\"efd2679c-4e14-11e4-8a57-0800271c1b75\",\"name\":{\"display\":\"Measles\",\"uuid\":\"60b55566-5900-4cc2-bdc4-55f717940cc6\",\"name\":\"Measles\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Measles\",\"resourceVersion\":\"2.0\",\"translationKey\":\"হাম_49\"},\"inactive\":false,\"groupMembers\":[]}");
			conjunctivitis = new JSONObject("{\"concept\":{\"uuid\":\"ed6dedbf-7bd3-4642-b497-0535e3ee1986\",\"name\":\"Disease_2Months_To_5Years_CHCP\"},\"formNamespace\":\"Bahmni\",\"formFieldPath\":\"শিশু (২ মাস থেকে ৫ বছর) স্বাস্থ্য সেবা.32/49-0\",\"voided\":false,\"value\":{\"uuid\":\"e2b14b2b-4e14-11e4-8a57-0800271c1b75\",\"name\":{\"display\":\"Conjunctivitis, unspec.\",\"uuid\":\"aa3e374b-fd72-4213-a796-478e614f6022\",\"name\":\"Conjunctivitis, unspec.\",\"locale\":\"en\",\"localePreferred\":true,\"conceptNameType\":null,\"resourceVersion\":\"1.9\"},\"displayString\":\"Conjunctivitis, unspec.\",\"resourceVersion\":\"2.0\",\"translationKey\":\"চোখ_উঠা_49\"},\"inactive\":false,\"groupMembers\":[]}");
			//malnutrition = new JSONObject("");
			//anemia = new JSONObject("");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(nameOfJSONObject.equals("normalDisease")){
			objectToReturn = normalDisease;
		}else if(nameOfJSONObject.equals("healthCareGivenYes")){
			objectToReturn = healthCareGivenYes;
		}else if(nameOfJSONObject.equals("healthCareGivenNo")){
			objectToReturn = healthCareGivenNo;
		}else if(nameOfJSONObject.equals("highBloodPressure")){
			objectToReturn = highBloodPressure;
		}else if(nameOfJSONObject.equals("diabetes")){
			objectToReturn = diabetes;
		}else if(nameOfJSONObject.equals("tuberculosis")){
			objectToReturn = tuberculosis;
		}else if(nameOfJSONObject.equals("otherPossibleDisease")){
			objectToReturn = otherPossibleDisease;
		}else if(nameOfJSONObject.equals("Union_Sub_Center")){
			objectToReturn = unionSubCenter;
		}else if(nameOfJSONObject.equals("Union_Family_Welfare_Center")){
			objectToReturn = unionFamilyWelfareCenter;
		}else if(nameOfJSONObject.equals("Union_Health_and_Family_Welfare_Center")){
			objectToReturn = unionHealthAndFamilyWelfareCenter;
		}else if(nameOfJSONObject.equals("Metarnal_and_Child_Wellfare_Center")){
			objectToReturn = metarnalAndChildWelfareCenter;
		}else if(nameOfJSONObject.equals("10_Bed_Hospital")){
			objectToReturn = tenBedHospital;
		}else if(nameOfJSONObject.equals("20_Beds_Hospital")){
			objectToReturn = twentyBedHospital;
		}else if(nameOfJSONObject.equals("Upazila_Health_Complex")){
			objectToReturn = upazilaHealthComplex;
		}else if(nameOfJSONObject.equals("District_Hospital")){
			objectToReturn = districtHospital;
		}else if(nameOfJSONObject.equals("Medical_College_and_Hospital")){
			objectToReturn = medicalCollegeAndHospital;
		}else if(nameOfJSONObject.equals("Others_Health_Facility")){
			objectToReturn = otherHealthFacility;
		}else if(nameOfJSONObject.equals("familyPlanningCHCP")){
			objectToReturn = familyPlanningCHCP;
		}else if(nameOfJSONObject.equals("oralContraceptives")){
			objectToReturn = oralContraceptives;
		}else if(nameOfJSONObject.equals("condoms")){
			objectToReturn = condoms;
		}else if(nameOfJSONObject.equals("injectable")){
			objectToReturn = injectable;
		}else if(nameOfJSONObject.equals("otherMethod")){
			objectToReturn = otherMethod;
		}else if(nameOfJSONObject.equals("haveDangerSignsPregnancyYes")){
			objectToReturn = haveDangerSignsPregnancyYes;
		}else if(nameOfJSONObject.equals("Bleeding_Through_Birth_Canal")){
			objectToReturn = bleedingThroughBirthCanal;
		}else if(nameOfJSONObject.equals("Prolonged_Delivery_or_Child_Coming_Out_Before")){
			objectToReturn = prolongedDelivery;
		}else if(nameOfJSONObject.equals("Edema")){
			objectToReturn = edema;
		}else if(nameOfJSONObject.equals("Jaundice")){
			objectToReturn = jaundice;
		}else if(nameOfJSONObject.equals("Convulsion")){
			objectToReturn = convulsion;
		}else if(nameOfJSONObject.equals("High_Temperature_102_Degree_or_More")){
			objectToReturn = highTemperature;
		}else if(nameOfJSONObject.equals("Weakness_Blurred_vision")){
			objectToReturn = weaknessBlurredVision;
		}else if(nameOfJSONObject.equals("Very_severe_disease")){
			objectToReturn = verySevereDisease;
		}else if(nameOfJSONObject.equals("Probable_Limited_Infection")){
			objectToReturn = probableLimitedInfection;
		}else if(nameOfJSONObject.equals("Bellybutton_infection")){
			objectToReturn = bellyButtonInfection;
		}else if(nameOfJSONObject.equals("Injury")){
			objectToReturn = injury;
		}else if(nameOfJSONObject.equals("Fever")){
			objectToReturn = fever;
		}else if(nameOfJSONObject.equals("Pneumonia")){
			objectToReturn = pneumonia;
		}else if(nameOfJSONObject.equals("coldAndCough")){
			objectToReturn = coldAndCough;
		}else if(nameOfJSONObject.equals("Diarrhoea_No_Dehydration")){
			objectToReturn = diarrhoeaNoDehydration;
		}else if(nameOfJSONObject.equals("Others_member_disease")){
			objectToReturn = othersMemberDisease;
		}else if(nameOfJSONObject.equals("dieriaanddysentry")){
			objectToReturn = diarrhoeaAndDysentery;
		}else if(nameOfJSONObject.equals("maleria")){
			objectToReturn = maleria;
		}else if(nameOfJSONObject.equals("hearingLoss")){
			objectToReturn = hearingLoss;
		}else if(nameOfJSONObject.equals("Measles")){
			objectToReturn = measles;
		}else if(nameOfJSONObject.equals("Conjunctivitis")){
			objectToReturn = conjunctivitis;
		}else if(nameOfJSONObject.equals("Malnutrition")){
			objectToReturn = malnutrition;
		}else if(nameOfJSONObject.equals("Anemia")){
			objectToReturn = anemia;
		}
		return objectToReturn;
	}
}
