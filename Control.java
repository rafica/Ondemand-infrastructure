import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;


public class Control {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		//Properties prop = new Properties();
		
		//input
		
		//prop.load(Control.class.getResourceAsStream("config.properties"));
		String Ip = null;
		String ami= null;
		Map<String,String> ip_dictionary = new HashMap <String,String>();
		Map<String,String> ami_dictionary = new HashMap <String,String>();
		ip_dictionary.put("1","54.221.210.42");
		ip_dictionary.put("2", "54.235.254.207");
		
		ami_dictionary.put("1","ami-db2678b2");
		ami_dictionary.put("2","ami-4de8b824");
		//load user to IP mapping from properties
		//String Ip = prop.getProperty(input);
		Scanner in = new Scanner(System.in);
		OnDemand ondemand_obj = new OnDemand();
		try{
			int i=0;
			while(i<2)
			{
			System.out.println("please enter your username");
			String input = in.nextLine();
			//String input = "1";
			Ip = ip_dictionary.get(input);
			ami = ami_dictionary.get(input);
			String new_ami = ondemand_obj.Ondemand_core_function(Ip,ami);
			ami_dictionary.put(input, new_ami);
			TimeUnit.MINUTES.sleep(2); //next day
			i++;
			
			}
			
		}
		catch(Exception e){}
		

	}



}
