package com.ikanow.aleph2.storm.samples.bolts;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SampleProxyLogEntry {	
	public static final String TIMESTAMP = "@timestamp";
	public static final String TIMESTAMP_MS = "timestamp_ms";
	public static final String DOCID = "docid";
	public static final String MESSAGE = "message";
	
	public static final String C_IP = "c_ip";
	public static final String CS_AUTH_GROUP = "cs_auth_group";
	public static final String CS_BYTES = "cs_bytes";
	public static final String CS_CATEGORIES = "cs_categories";
	public static final String CS_HOST = "cs_host";
	public static final String CS_METHOD = "cs_method";
	public static final String CS_REFERRER = "cs_referrer";
	public static final String CS_URI_EXTENSION = "cs_uri_extension";
	public static final String CS_URI_PATH = "cs_uri_path";
	public static final String CS_URI_PORT = "cs_uri_port";
	public static final String CS_URI_QUERY = "cs_uri_query";
	public static final String CS_URI_SCHEME = "cs_uri_scheme";
	public static final String CS_USER_AGENT = "cs_user_agent";
	public static final String CS_USERNAME = "cs_username";
	public static final String R_IP = "r_ip";
	public static final String RS_CONTENT_TYPE = "rs_content_type";
	public static final String S_ACTION = "s_action";
	public static final String S_ICAP_STATUS = "s_icap_status";
	public static final String S_IP = "s_ip";
	public static final String S_SUPPLIER_NAME = "s_supplier_name";
	public static final String SC_BYTES = "sc_bytes";
	public static final String SC_FILTER_RESULT = "sc_filter_result";
	public static final String SC_STATUS = "sc_status";
	public static final String TIME_TAKEN = "time_taken";
	public static final String X_EXCEPTION_ID = "x_exception_id";
	public static final String X_VIRUS_ID = "x_virus_id";

	private static SimpleDateFormat date_formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static String[] param_name_list = new String[]{"date","date_time","time_taken","c_ip","cs_username","cs_auth_group","cs_method","cs_bytes","cs_uri_scheme","cs_host","cs_uri_path","cs_uri_port","cs_uri_query","cs_categories","cs_referrer","cs_user_agent","cs_uri_extension","r_ip","sc_bytes","sc_filter_result","rs_content_type","sc_status","s_ip","s_action","s_supplier_name","s_icap_status","x_virus_id","x_exception_id"};
	private static final Map<String, Integer> param_name_map = new HashMap<String, Integer>();
	static {
		//set up param name map;
		param_name_map.put("date", 0);
		param_name_map.put("date_time", 1);
		param_name_map.put(TIME_TAKEN, 2);
		param_name_map.put(C_IP, 3);
		param_name_map.put(CS_USERNAME, 4);
		param_name_map.put(CS_AUTH_GROUP, 5);
		param_name_map.put(CS_METHOD, 6);
		param_name_map.put(CS_BYTES, 7);
		param_name_map.put(CS_URI_SCHEME, 8);
		param_name_map.put(CS_HOST, 9);
		param_name_map.put(CS_URI_PATH, 10);
		param_name_map.put(CS_URI_PORT, 11);
		param_name_map.put(CS_URI_QUERY, 12);
		param_name_map.put(CS_CATEGORIES, 13);
		param_name_map.put(CS_REFERRER, 14);
		param_name_map.put(CS_USER_AGENT, 15);
		param_name_map.put(CS_URI_EXTENSION, 16);
		param_name_map.put(R_IP, 17);
		param_name_map.put(SC_BYTES, 18);
		param_name_map.put(SC_FILTER_RESULT, 19);
		param_name_map.put(RS_CONTENT_TYPE, 20);
		param_name_map.put(SC_STATUS, 21);
		param_name_map.put(S_IP, 22);
		param_name_map.put(S_ACTION, 23);
		param_name_map.put(S_SUPPLIER_NAME, 24);
		param_name_map.put(S_ICAP_STATUS, 25);
		param_name_map.put(X_VIRUS_ID, 26);
		param_name_map.put(X_EXCEPTION_ID, 27);
	}

	public static String getParamName(int token_counter) {
		return param_name_list[token_counter];
	}

	public String c_ip;
	public String cs_auth_group;
	public int cs_bytes;
	public String cs_categories;
	public String cs_host;
	public String cs_method;
	public String cs_referrer;
	public String cs_uri_extension;
	public String cs_uri_path;
	public String cs_uri_port;
	public String cs_uri_query;
	public String cs_uri_scheme;
	public String cs_user_agent;
	public String cs_username;
	private String[] param_values = new String[28];
	public String r_ip;
	public String rs_content_type;
	public String s_action;
	public String s_icap_status;
	public String s_ip;
	public String s_supplier_name;
	public String sc_bytes;	
	public String sc_filter_result;
	public String sc_status;
	public int time_taken;
	public String x_exception_id;
	public String x_virus_id;

	/**
	 * takes the string token and sets the correct
	 * variable based on token_counter
	 * 
	 * @param token
	 * @param token_counter
	 */
	public void setParam(String token, int token_counter) 
	{
		param_values[token_counter] = token;
	}

	/**
	 * Transfer all the input values to this objects variables
	 * 
	 * @throws ParseException
	 */
	public void setVars() throws Exception
	{
		this.timestamp = date_formatter.parse(param_values[0] + " " + param_values[1]);
		this.time_taken = Integer.parseInt(param_values[2]);
		this.c_ip = param_values[3];
		this.cs_username = param_values[4];
		this.cs_auth_group = param_values[5];
		this.cs_method = param_values[6];
		this.cs_bytes = Integer.parseInt( param_values[7] );
		this.cs_uri_scheme = param_values[8];
		this.cs_host = param_values[9];
		this.cs_uri_path = param_values[10];
		this.cs_uri_port = param_values[11];
		this.cs_uri_query = param_values[12];
		this.cs_categories = param_values[13];
		this.cs_referrer = param_values[14];
		this.cs_user_agent = param_values[15];
		this.cs_uri_extension = param_values[16];
		this.r_ip = param_values[17];
		this.sc_bytes = param_values[18];
		this.sc_filter_result = param_values[19];
		this.rs_content_type = param_values[20];
		this.sc_status = param_values[21];
		this.s_ip = param_values[22];
		this.s_action = param_values[23];
		this.s_supplier_name = param_values[24];
		this.s_icap_status = param_values[25];
		this.x_virus_id = param_values[26];
		this.x_exception_id = param_values[27];
		param_values = null;
	}
	
	/**
	 * Date for the entry or the date of parse.
	 */
	public Date timestamp;
	/**
	 * The id unique to the type
	 */
	public String id;
	/**
	 * The original message
	 */
	public String message;
	
	/**
	 * The milliseconds from the timestamp (e.g. If timestamp is "11:01:01.999", this value shoud be 999)
	 */
	public long timestamp_ms;
}
