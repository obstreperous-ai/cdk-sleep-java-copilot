package com.myorg;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CdkBaseTest {
    @Test
    void synthesizesAnEmptyBootstrapStack() {
        App app = new App();
        CdkBaseStack stack = new CdkBaseStack(app, "test");

        Template template = Template.fromStack(stack);

        assertEquals(0, template.findResources("AWS::SQS::Queue").size());
    }
}
