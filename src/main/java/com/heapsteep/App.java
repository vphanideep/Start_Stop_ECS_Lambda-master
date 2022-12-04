package com.heapsteep;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import com.amazonaws.services.ecs.model.UpdateServiceResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;


public class App implements RequestHandler<Object,Object>{
	private final static Logger LOGGER = Logger.getLogger(App.class.getName());
		
	public Object handleRequest(Object input, Context context) {
		
		String resource=(String)((List)((LinkedHashMap)input).get("resources")).get(0);//Get the value of the 'resources' attribute from input event.
		String[] resourceArr=resource.split("/");
		AmazonECS client=AmazonECSClientBuilder.standard().withRegion("ap-south-1").build();
		
		Yaml yaml=new Yaml();
		InputStream is=this.getClass().getClassLoader().getResourceAsStream("application.yaml");		
		
		List fullList=null;
		
		Object obj=yaml.load(is);
		LOGGER.info("application.yaml values: "+obj.toString());
		fullList=(ArrayList)obj;
		
		String finalMessage=null;
		for (Object obj2 : fullList) { 
			Map map=(Map)obj2;
			Iterator it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry set = (Map.Entry) it.next();
				String clusterName=(String)set.getKey();
				List servicesList=(List)set.getValue();
				for (Object obj3 : servicesList) { 
					Map map2=(Map)obj3;	
					Iterator it2 = map2.entrySet().iterator();
					while (it2.hasNext()) {
						Map.Entry set2 = (Map.Entry) it2.next();
						LOGGER.info("Cluster:" + clusterName + ",Service:"+set2.getKey()+",desiredCount:"+set2.getValue());
						
						if(resourceArr[1].equals("Stop_ECS")) {
							UpdateServiceRequest request=new UpdateServiceRequest().withCluster(clusterName).withService(set2.getKey().toString()).withDesiredCount(0);
							
							UpdateServiceResult response=client.updateService(request);	       
					        LOGGER.info("===>Message:" + response.getService().getEvents().get(3).getMessage());
					        					        
							finalMessage= "*******All ECS Services are Stopped as part of Weekend Shutdown********";
						}else if(resourceArr[1].equals("Start_ECS")) {
							UpdateServiceRequest request=new UpdateServiceRequest().withCluster(clusterName).withService(set2.getKey().toString()).withDesiredCount((Integer)set2.getValue());
							
							UpdateServiceResult response=client.updateService(request);			
					        LOGGER.info("===>Message:" + response.getService().getEvents().get(3).getMessage());
					        					       
					        finalMessage= "*******All ECS Services are Started as part of Weekday Startup*******";
						}						
					}
				}
			}  		
	    }					
		LOGGER.info(finalMessage);
		return finalMessage;
	}
}
