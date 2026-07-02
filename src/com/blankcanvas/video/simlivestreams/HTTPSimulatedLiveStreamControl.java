package com.blankcanvas.video.simlivestreams;

import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.http.HTTProvider2Base;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.publish.Publisher;
import com.wowza.wms.stream.publish.Stream;
import com.wowza.wms.util.StreamUtils;
import com.wowza.wms.vhost.IVHost;

public class HTTPSimulatedLiveStreamControl extends HTTProvider2Base
{
	public static final String MODULE_NAME = "HTTPSimulatedLiveStreamControl";
	private static final Class<HTTPSimulatedLiveStreamControl> CLASS = HTTPSimulatedLiveStreamControl.class;
	private WMSLogger logger = WMSLoggerFactory.getLogger(CLASS);

	private static final String PARAM_APP_NAME = "app";
	private static final String PARAM_APP_INSTANCE_NAME = "appInst";
	private static final String PARAM_ACTION = "action";
	private static final String PARAM_STREAM_NAME = "stream";
	private static final String PARAM_VOD_NAME = "vod";

	private static final String ACTION_STOP_STREAM = "stop";
	private static final String ACTION_START_STREAM = "start";
	private static final String ACTION_LIST_STREAMS = "list";

    private List<String> streamList = new CopyOnWriteArrayList<>();  // assumes 1 app currently


	@Override
	public void init()
	{
		super.init();		
		logger.warn(MODULE_NAME+" .init");
	}

	@Override
	public void onHTTPRequest(IVHost vhost, IHTTPRequest req, IHTTPResponse resp)
	{
//		logger.warn(MODULE_NAME+" .onHTTPRequest");
//		for (String p : req.getParameterNames()) {
//			logger.info("req: " + p + ":" + req.getParameter(p));				
//		}
		
		if (!doHTTPAuthentication(vhost, req, resp))
			return;

		StringBuffer ret = new StringBuffer();
		Set<String> paramNames = req.getParameterNames();
		IApplicationInstance appInstance = null;
		String actionType = ACTION_LIST_STREAMS;
		String streamName = null;
		String vodName = "sample.mp4";


		if (paramNames.contains(PARAM_APP_NAME))
		{
			String appName = req.getParameter(PARAM_APP_NAME);
			//logger.warn(MODULE_NAME+" .onHTTPRequest app:"+appName);

			String appInstanceName = req.getParameter(PARAM_APP_INSTANCE_NAME);
			appInstanceName = appInstanceName == null ? "_definst_" : appInstanceName;
			try
			{
				appInstance = vhost.getApplication(appName).getAppInstance(appInstanceName);
			}
			catch (Exception e)
			{
				logger.error("Invalid appInstance: " + e);
			}

			//logger.warn(MODULE_NAME+" .onHTTPRequest appInstance:"+appInstance);
			if (appInstance != null)
			{
				if (paramNames.contains(PARAM_ACTION))
					actionType = req.getParameter(PARAM_ACTION);
				
				if (paramNames.contains(PARAM_STREAM_NAME))
					streamName = req.getParameter(PARAM_STREAM_NAME);
				
				if (paramNames.contains(PARAM_VOD_NAME))
					vodName = req.getParameter(PARAM_VOD_NAME);

				logger.warn(MODULE_NAME+" .onHTTPRequest app:"+appName+ " action:"+actionType+ " stream:"+streamName+" vod:"+vodName);

				
				if (actionType.equalsIgnoreCase(ACTION_START_STREAM) && streamName != null)
					startStream(appInstance, streamName, vodName);
				else if (actionType.equalsIgnoreCase(ACTION_STOP_STREAM) && streamName != null)
					stopStream(appInstance, streamName);
				else if (actionType.equalsIgnoreCase(ACTION_LIST_STREAMS))
					listStreams(appInstance);
				
				else
					ret.append("Invalid or missing argument(s).");
			}
			else
				ret.append("Requires a valid application name.");
		}
		else
			ret.append("Requires an application name.");

		try
		{
			OutputStream out = resp.getOutputStream();
			byte[] outBytes = ret.toString().getBytes();
			out.write(outBytes);
		}
		catch (Exception e)
		{
			logger.error(e);
		}
		//logger.warn(MODULE_NAME+" .onHTTPRequest END");
	}

	public void startStream(IApplicationInstance appInstance, String streamName, String vodName)
	{		
		//logger.info(MODULE_NAME + " > START: stream=" + streamName+" : "+vodName);		
		
        Stream stream = (Stream) appInstance.getProperties().get(streamName);
        if (stream != null)
        {
            logger.warn(MODULE_NAME + ".startStream [" + appInstance.getContextStr() + "/" + streamName + "] stream already running");
            return;
        }
        double streamLength = StreamUtils.getStreamLength(appInstance, vodName);
		//logger.info(MODULE_NAME + " > START: stream=" + streamLength+" : "+streamLength);		
		if (streamLength <= 0)
        {
            logger.warn(MODULE_NAME + ".startStream [" + appInstance.getContextStr() + "/" + streamName + "] vod file not found: " + vodName);
            return;
        }
        
        stream = Stream.createInstance(appInstance, streamName);
        stream.play(vodName, 0, -1, true);
        appInstance.getProperties().setProperty(streamName, stream);
        if(!streamList.contains(streamName))
            streamList.add(streamName);

		logger.info(MODULE_NAME + " START: stream=" + streamName+" : "+vodName);
	}
	
	public void stopStream(IApplicationInstance appInstance, String streamName)
	{
		logger.info(MODULE_NAME + " STOP : stream=" + streamName);
		
        Stream stream = (Stream) appInstance.getProperties().remove(streamName);
        if(stream != null)
        {
            stream.closeAndWait();
            Publisher publisher = stream.getPublisher();
            if(publisher != null)
                publisher.close();
        }
        streamList.remove(streamName);

	}

	public void listStreams(IApplicationInstance appInstance)
	{		
		if (streamList.isEmpty()) {
			logger.info(MODULE_NAME + " LIST : none");			
			return;
		}
		
		String streams = "";
		for (String s : streamList) {
			streams += s + ", ";			
		}
		logger.info(MODULE_NAME + " LIST : " + streams);
		
	}


}
