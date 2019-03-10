package genunit;

import static org.apache.commons.lang3.StringUtils.uncapitalize;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import java.util.ArrayList;
import java.util.List;

public class FieldDeclarationVisitor extends ModifierVisitor<Void> {
    @Override
    public Visitable visit(FieldDeclaration n, Void arg) {
        List fieldDeclarations = (List) MetadataHolder.getMetadata().get("fieldDeclarations");
        if (fieldDeclarations == null) {
            fieldDeclarations = new ArrayList();
            MetadataHolder.getMetadata().put("fieldDeclarations", fieldDeclarations);
        }
        ClassOrInterfaceType fieldType = (ClassOrInterfaceType) n.getVariables().get(0).getType();
        String fieldTypeName = fieldType.getName().asString();
        String variableName = uncapitalize(fieldTypeName);
        fieldDeclarations.add(new FieldDeclarationMeta(fieldTypeName, variableName));
        return super.visit(n, arg);
    }
}
