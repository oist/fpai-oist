package org.flexiblepower.driver.generator;

import org.flexiblepower.ral.ResourceState;

public interface GeneratorState extends ResourceState {

    public GeneratorLevel getGeneratorLevel();
}