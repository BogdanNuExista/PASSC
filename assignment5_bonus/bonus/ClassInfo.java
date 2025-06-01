import org.w3c.dom.Element;
import java.util.List;

public class ClassInfo {
    public String className;
    public Element typeElement;
    public List<Field> fields;

    public ClassInfo(String className, Element typeElement) {
        this.className = className;
        this.typeElement = typeElement;
    }
}