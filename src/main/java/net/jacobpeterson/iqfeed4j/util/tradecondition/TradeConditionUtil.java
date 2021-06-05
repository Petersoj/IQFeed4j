package net.jacobpeterson.iqfeed4j.util.tradecondition;

import java.util.LinkedList;
import java.util.List;

/**
 * {@link TradeConditionUtil} contains utility methods for trade conditions.
 */
public class TradeConditionUtil {

    /**
     * Gets a list of base-10 integers from a non-delimited list of 2-digit hexadecimal numbers (with no '0x' prefix)
     * which represent various trade conditions.
     *
     * @param tradeConditionString the non-delimited list of hexadecimal trade conditions
     *
     * @return a new {@link List} or <code>null</code> if no digits were converted
     */
    public static List<Integer> listFromTradeConditionString(String tradeConditionString) {
        if (tradeConditionString == null || tradeConditionString.isEmpty()) {
            return null;
        }

        LinkedList<Integer> tradeConditions = new LinkedList<>();

        for (int index = 0; index < tradeConditionString.length(); index++) {
            if (index + 1 < tradeConditionString.length()) { // Check if there is another hex digit after current
                tradeConditions.add(Integer.parseInt(tradeConditionString.substring(index, index + 2), 16));
                index++;
            } else {
                throw new IllegalArgumentException("Only found one hex digit for trade condition at index " + index);
            }
        }

        return tradeConditions;
    }
}
