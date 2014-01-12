import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.CreateSnapshotRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.DetachVolumeRequest;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.ec2.model.VolumeAttachment;


public class OnDemand {

	
		 
	public String Ondemand_core_function(String elasticIp, String ami)throws Exception{

		String imageId = null;
		
		
		/*********************************************
 		*  	#1 Create Amazon Client object
 		*********************************************/
		AWSCredentials credentials = new PropertiesCredentials(
				OnDemand.class.getResourceAsStream("AwsCredentials.properties"));
	 
 		System.out.println("#1 Create Amazon Client object");
 		AmazonEC2 ec2 = new AmazonEC2Client(credentials);
 		
 		
 		/**********************************************
 		 * 
 		 * #2 Create Instance from AMI
 		 * 
 		 **********************************************/
 		// CREATE MAPPING WITH ELASTIC IP AND IMAGE ID IN PROPERTIES
 		//retrieve ami from properties
 		
 		//Properties p = new Properties();
 		//p.load(OnDemand.class.getResourceAsStream("config.properties"));
 		
 		//String ami = p.getProperty(elasticIp);
 		//String ami="ami-35792c5c";
 		//ami = "ami-76f0061f";
 		System.out.println("ami is"+ami);
 		RunInstancesRequest rir = new RunInstancesRequest();
 		System.out.println(rir);
         
         rir.withImageId(ami).withInstanceType("t1.micro")
         .withMinCount(1)
         .withMaxCount(1)
         .withKeyName("PratikKey")
         .withSecurityGroups("PratikSecurityGroup");
         
         RunInstancesResult resultinstance = ec2.runInstances(rir);
         
         //get instanceId from the result
         List<Instance> resultInstance = resultinstance.getReservation().getInstances();
         String createdInstanceId = null;
         for (Instance ins : resultInstance){
         	createdInstanceId = ins.getInstanceId();
         	System.out.println("New instance has been created: "+ins.getInstanceId());

	 }
		
 		String instanceId = createdInstanceId; //put your own instance id to test this code.
 		//String instanceId = "i-94ae7df0";
		try{
 			
			/*********************************************
			*  	#2 Allocate elastic IP addresses.
			*********************************************/
			TimeUnit.MINUTES.sleep(1);
			//allocate
			/*AllocateAddressResult elasticResult = ec2.allocateAddress();
			String elasticIp = elasticResult.getPublicIp();
			System.out.println("New elastic IP: "+elasticIp);
				*/
			//associate
			AssociateAddressRequest aar = new AssociateAddressRequest();
			aar.setInstanceId(instanceId);
			
			aar.setPublicIp(elasticIp);
			System.out.println(aar);
			ec2.associateAddress(aar);
			System.out.println("associated");
			
			//disassociate
			/*DisassociateAddressRequest dar = new DisassociateAddressRequest();
			dar.setPublicIp(elasticIp);
			ec2.disassociateAddress(dar);
          */
        	
			/***********************************
			 *   #3 Monitoring (CloudWatch)
			 *********************************/
		
		int to_break=0;
		while(true)
		{
			System.out.println("in while");

			//create CloudWatch client
			AmazonCloudWatchClient cloudWatch = new AmazonCloudWatchClient(credentials) ;
			
			//create request message
			GetMetricStatisticsRequest statRequest = new GetMetricStatisticsRequest();
			
			//set up request message
			statRequest.setNamespace("AWS/EC2"); //namespace
			statRequest.setPeriod(60); //period of data
			ArrayList<String> stats = new ArrayList<String>();
			
			//Use one of these strings: Average, Maximum, Minimum, SampleCount, Sum 
			stats.add("Average"); 
			stats.add("Sum");
			statRequest.setStatistics(stats);
			
			//Use one of these strings: CPUUtilization, NetworkIn, NetworkOut, DiskReadBytes, DiskWriteBytes, DiskReadOperations  
			statRequest.setMetricName("CPUUtilization"); 
			
			// set time
			GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
			calendar.add(GregorianCalendar.SECOND, -1 * calendar.get(GregorianCalendar.SECOND)); // 1 second ago
			Date endTime = calendar.getTime();
			calendar.add(GregorianCalendar.MINUTE, -10); // 10 minutes ago
			Date startTime = calendar.getTime();
			statRequest.setStartTime(startTime);
			statRequest.setEndTime(endTime);
			
			//specify an instance
			ArrayList<Dimension> dimensions = new ArrayList<Dimension>();
			dimensions.add(new Dimension().withName("InstanceId").withValue(instanceId));
			statRequest.setDimensions(dimensions);
			
			//get statistics
			GetMetricStatisticsResult statResult = cloudWatch.getMetricStatistics(statRequest);
			
			//display
			System.out.println(statResult.toString());
			List<Datapoint> dataList = statResult.getDatapoints();
			Double averageCPU = null;
			Date timeStamp = null;
			
			for (Datapoint data : dataList){
				averageCPU = data.getAverage();
				timeStamp = data.getTimestamp();
				if (averageCPU < 1)
				{
					System.out.println("system idle for last ten min. instance going to get terminated");
					to_break = 1;
					break;	
				}
				
				System.out.println("Average CPU utlilization for last 10 minutes: "+averageCPU);
				System.out.println("Totl CPU utlilization for last 10 minutes: "+data.getSum());
			}
			
			
			Calendar now = new GregorianCalendar();
			int hour = now.get(Calendar.HOUR);
			if(hour>=8) // change to 5 railway time
			{
				to_break=1;
			}
			
			if (to_break ==1)
			{
				break;
			}
			
			try{
				System.out.println("sleeping for ten min");
				TimeUnit.MINUTES.sleep(10);

			}catch(Exception e){
				System.out.println("interrupted");
				System.out.println(e.getMessage());
				
			}
            
			
			
		}
		
		
		
		if(to_break == 1 )
		{   
			// Get volumeID
			 DescribeVolumesResult result=ec2.describeVolumes(new DescribeVolumesRequest());
			 
			 String volumeId=null;
			 List<Volume> volumelist=result.getVolumes();
			 Iterator<Volume> iter = volumelist.iterator();
			 volumeId = result.getVolumes().get(0).getVolumeId(); 
			 while(iter.hasNext())
			 {
				 Volume vol=iter.next();
				 List<VolumeAttachment> att_list=vol.getAttachments();
				 if(!att_list.isEmpty())
				 {
					 VolumeAttachment att=att_list.get(0);
					 System.out.println(att_list.get(0));
					 if(att.getInstanceId().equals(instanceId)&&att.getDeleteOnTermination().equals(false))
					 	{
						 volumeId = att.getVolumeId();
					 	}
				 
				 
				 }
			 }

			 
			 
			// Detach volume
			 System.out.println(ec2.detachVolume(new DetachVolumeRequest().withInstanceId(instanceId).withVolumeId(volumeId)));
			 System.out.println("Volume detatched");
			 
			 
			 // Take snapshot of the volume
			 ec2.createSnapshot(new CreateSnapshotRequest().withVolumeId(volumeId).withDescription("created programmatically"));
			 System.out.println("snapshot created");
			 
			 // take AMI
			final CreateImageResult createAMIResult = ec2.createImage(new CreateImageRequest().withInstanceId(instanceId).withName("MyAmi").withNoReboot(false));
			imageId = createAMIResult.getImageId();
			System.out.println("ami created "+imageId);
			
			
			//Properties pro = new Properties();
			//pro.setProperty("image2",imageId);
			//pro.store(new FileOutputStream("config.properties"), null);
		
			
			//out.close();
			//STORE IT IN PROPERTIES FILE
			
			
			 
			// Delete the instance
			ec2.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceId));
			System.out.println("instance terminated");
			
			
		}
            
		} catch (AmazonServiceException ase) {
		    System.out.println("Caught Exception: " + ase.getMessage());
		    System.out.println("Reponse Status Code: " + ase.getStatusCode());
		    System.out.println("Error Code: " + ase.getErrorCode());
		    System.out.println("Request ID: " + ase.getRequestId());
		}
		return imageId;
	        
	}
	
}