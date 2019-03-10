package ${packageName};

import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

<#list fieldDeclarations as fieldDeclaration>
<#if fieldDeclaration.fullFieldType??>
import ${fieldDeclaration.fullFieldType};
</#if>
</#list>

<#list methodDeclarations as methodDeclaration>
<#list methodDeclaration.params as methodParam>
<#if methodParam.fullFieldType??>
import ${methodParam.fullFieldType};
</#if>
</#list>
</#list>

public class ${className}Test {
<#list fieldDeclarations as fieldDeclaration>
  ${fieldDeclaration.fieldType} ${fieldDeclaration.fieldName};
</#list>

  ${className} ${classNameVar};

  @BeforeEach
  public void init() {
<#list fieldDeclarations as fieldDeclaration>
    ${fieldDeclaration.fieldName} = mock(${fieldDeclaration.fieldType}.class);
</#list>
    ${classNameVar} = new ${className}(${constructorArgs});
  }

<#list methodDeclarations as methodDeclaration>
  @Test
  public void ${methodDeclaration.methodName}() {
<#list methodDeclaration.params as methodParam>
    ${methodParam.fieldType} ${methodParam.fieldName} = null;
</#list>
    ${classNameVar}.${methodDeclaration.methodName}(${methodDeclaration.methodArgs});
  }

</#list>
}
