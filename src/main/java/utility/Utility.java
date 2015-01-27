package utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * intended to contain miscellaneous helper functions.
 * @author vancan1ty
 *
 */
public class Utility {
	
	/**
	 * This function returns a new list with the contents of the input
	 * list in reverse order.
	 * @param input
	 * @return
	 */
	public static <E> List<E> reverseList(List<E> input) {
		if (input == null) {
			return null;
		}
		List output = new ArrayList(input.size());
		for (int i = input.size()-1; i>=0;i--) {
			output.add(input.get(i));
		}
		return output;
	}
	
	/**
	 * this is used to uniformly format ======= type headers in our output.
	 * @param message
	 * @return
	 */
	public static String formatHeaderForOutput(String message) {
		int outWidth = 42;
		boolean messageIsEmpty = (message == null || message.length()==0);
		int messageLength;
		if(message==null) {
			messageLength = 0;
		} else {
			messageLength = message.length();
		}

		int numSpacesToAdd = messageIsEmpty ? 0 : 2;
		int numOfEqualsChars = Math.max(outWidth - messageLength - numSpacesToAdd,0);
		int numOfEqualsOnLeft = (int) Math.ceil(numOfEqualsChars/2.0);
		int numOfEqualsOnRight = (int) Math.floor(numOfEqualsChars/2.0);
		StringBuffer out = new StringBuffer();
		for(int i = 0; i < numOfEqualsOnLeft; i++) {
			out.append("=");
		}
		if(!messageIsEmpty) {
            out.append(" ");
            out.append(message);
            out.append(" ");
		}
		for(int i = 0; i < numOfEqualsOnRight; i++) {
			out.append("=");
		}
		return out.toString();
	}
	
	/**
	 * handy method to initialize a map like you would in a sane language
	 * (i.e. not java)
	 */
	public static <K,V> Map<K,V> createMapFromArray(Object ... items) {
		if(items.length % 2 != 0) {
			throw new RuntimeException("invalid map size!");
		}

		HashMap<K,V> out = new HashMap();
		for (int i = 0; i < items.length; i+=2) {
			Object key = items[i];
			Object value = items[i+1];

			out.put((K) key, (V) value);
		}
		
		return out;
	}

}
