package com.nordstrom.automation.junit;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import com.nordstrom.common.base.UncheckedThrow;

/**
 * This is a static utility class that uses reflection to access the list of {@link TestRule} objects inside a
 * {@link RuleChain}.
 * 
 * @deprecated Use the <a href='https://junit.org/junit4/javadoc/4.13.1/org/junit/Rule.html#order()'>order</a>
 *             parameter of the {@literal @}Rule annotation instead of {@link RuleChain}.
 */
@Deprecated
public class RuleChainWalker {
    
    private RuleChainWalker() {
        throw new AssertionError("RuleChainWalker is a static utility class that cannot be instantiated");
    }
    
    /**
     * Get reference to an instance of the specified test rule type on the supplied rule chain.
     * 
     * @param <T> test rule type
     * @param ruleChain rule chain to be walked
     * @param ruleType test rule type
     * @return optional test rule instance
     */
    @SuppressWarnings("unchecked")
    public static <T extends TestRule> Optional<T> getAttachedRule(RuleChain ruleChain, Class<T> ruleType) {
        for (TestRule rule : getRuleList(ruleChain)) {
            if (rule.getClass() == ruleType) {
                return Optional.of((T) rule);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Get the list of test rules from the specified rule chain.
     * 
     * @param ruleChain rule chain
     * @return list of test rules
     */
    @SuppressWarnings("unchecked")
    private static List<TestRule> getRuleList(RuleChain ruleChain) {
        Field ruleChainList;
        try {
            ruleChainList = RuleChain.class.getDeclaredField("rulesStartingWithInnerMost");
            ruleChainList.setAccessible(true);
            return (List<TestRule>) ruleChainList.get(ruleChain);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw UncheckedThrow.throwUnchecked(e);
        }
    }

}
