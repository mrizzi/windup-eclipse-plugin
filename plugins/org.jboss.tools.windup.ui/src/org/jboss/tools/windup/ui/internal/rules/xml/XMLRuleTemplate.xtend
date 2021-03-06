package org.jboss.tools.windup.ui.internal.rules.xml

import java.util.List

class XMLRuleTemplate {
    
    public static interface Template {
        def String getName();
        def String generate();
    }
    
    def List<Template> getTemplates() {
        var templates = newArrayList
        templates.add(new StubRuleTemplate())
        templates
    }
    
    public static class StubRuleTemplate implements Template {
        override String getName() {
            "stub"
        }
        override String generate() {
            '''
            <rule id="1">
                <!-- rule condition, when it could be fired -->
                <when>
                </when>
                <!-- rule operation, what to do if it is fired -->
                <perform>
                </perform>
            </rule>
            ''' 
        }
    }
}