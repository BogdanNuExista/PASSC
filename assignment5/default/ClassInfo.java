import org.w3c.dom.Element;
import java.util.List;

public class ClassInfo {
    public String className;
    public Element complexTypeElement;
    public List<Field> fields;

    public ClassInfo(String className, Element complexTypeElement) {
        this.className = className;
        this.complexTypeElement = complexTypeElement;
    }
}