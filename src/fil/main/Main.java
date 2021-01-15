/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/

package fil.main;

import java.util.Scanner;

import fil.algorithm.BLFF.BLFF;
import fil.algorithm.BLLL.BLLL;
import fil.algorithm.BLRESCE.BLRESCE;
import fil.algorithm.RESCE.RESCE;
import fil.algorithm.RESCEFF.RESCEFF;
import fil.algorithm.RESCELL.RESCELL;
import fil.algorithm.SFCCMFF.SFCCMFF;
import fil.algorithm.SFCCMLL.SFCCMLL;
import fil.algorithm.SFCCMRESCE.SFCCMRESCE;
import fil.resource.virtual.GenRequest;

public class Main {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("BK-EdgeCloud Simulator 2020");
		System.out.println("");

		BLFF blFF = new BLFF();
		BLLL blLL = new BLLL();
		BLRESCE blRESCE = new BLRESCE(); // BL-VNFm
		RESCEFF resceFF = new RESCEFF(); // Ecoff-FF

		RESCELL resceLL = new RESCELL(); // Ecoff-LL
		RESCE resce = new RESCE(); 

		SFCCMFF sfccmFF = new SFCCMFF();
		SFCCMLL sfccmLL = new SFCCMLL();
		SFCCMRESCE sfccmRESCE = new SFCCMRESCE(); // SFCCM-VNFm
		
		//<-----------generate list of request
		GenRequest sample = new GenRequest();
		Scanner scanner = new Scanner(System.in);
		System.out.println("Input '0' for OfferedLoad increase option. --> Default.");
		System.out.println("Input '1' for 24h simulation option.");
		System.out.println("");
		System.out.print("Input: ");

		int req_opt = 0; int algo_opt = 0;
		req_opt = scanner.nextInt();
		sample.generator(req_opt);
		
		System.out.print("");
		if(req_opt != 1)
			System.out.println("Option '0' is chosen.");
		else
			System.out.println("Option '1' is chosen.");
		System.out.println("Choose option for running algorithm:");
		System.out.println("");
		System.out.println("0: All algorithms --> Default, take longest time.");
		System.out.println("1: RESCE only.");
		System.out.println("2: RESCE, ECoff-FF and ECoff-LL for 24h energy comparison.");
		System.out.println("");
		System.out.print("Input: ");

		algo_opt = scanner.nextInt();
//        scanner.nextLine(); 
		 
		//<-----------run all algorithm here
        if(algo_opt == 1) {
    		System.out.println("Option 1: Only RESCE.");
    		resce.run(sample.getListEventRep());
        } else if (algo_opt == 2) {
    		System.out.println("Option 2: RESCE, ECoff-FF and ECoff-LL.");
        	resce.run(sample.getListEventRep());
    		resceLL.run(sample.getListEventRep());
    		resceFF.run(sample.getListEventRep());
        }else {
    		System.out.println("Option default: All algorithms.");
//
        	blFF.run(sample.getListEventRep());
    		blLL.run(sample.getListEventRep());
    		blRESCE.run(sample.getListEventRep());

    		resce.run(sample.getListEventRep());
    		resceLL.run(sample.getListEventRep());
    		resceFF.run(sample.getListEventRep());
//           
    		sfccmFF.run(sample.getListEventRep());
    		sfccmLL.run(sample.getListEventRep());
    		sfccmRESCE.run(sample.getListEventRep());
        }
        scanner.close();
		System.out.println("Done.");
		 System.exit(0);
	}
}
