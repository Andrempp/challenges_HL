package utils.others;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RemovalUtils {

  public static List<Integer> getItemsToRemove(int nritems, String removals, boolean symmetric) {
	  List<Integer> remItems = new ArrayList<Integer>();
		if(removals.equals("Zero-Entries")){
			if(symmetric && nritems>5) return Arrays.asList(-1,0,1);
			if(!symmetric && nritems>4) return Arrays.asList(0,1);
			return Arrays.asList(0);
		} else if(!removals.equals("None")){
			if(symmetric){
				for(int i=-nritems/2+1; i<nritems/2; i++) remItems.add(i);
				if(remItems.size()==0) remItems.add(0);
			} else {
				for(int i=0; i<nritems-1; i++) remItems.add(i);
			}
		}
	return remItems;
}

}
