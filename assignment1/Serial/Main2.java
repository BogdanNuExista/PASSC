package Serial;

import java.util.*;

class Message {
    private String username;
    private String product;
    private String reviewText;
    private String attachment;
    
    private boolean isActive = true;
    private boolean buyerChecked = false;
    private boolean profanityChecked = false;
    private boolean imageResized = false;
    private boolean sentimentAnalyzed = false;

    public Message(String username, String product, String reviewText, String attachment) {
        this.username = username;
        this.product = product;
        this.reviewText = reviewText;
        this.attachment = attachment;
    }
    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    @Override
    public String toString() {
        return "Message{" +
                "username='" + username + '\'' +
                ", product='" + product + '\'' +
                ", reviewText='" + reviewText + '\'' +
                ", attachment='" + attachment + '\'' +
                '}';
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isBuyerChecked() {
        return buyerChecked;
    }

    public void setBuyerChecked(boolean buyerChecked) {
        this.buyerChecked = buyerChecked;
    }

    public boolean isProfanityChecked() {
        return profanityChecked;
    }

    public void setProfanityChecked(boolean profanityChecked) {
        this.profanityChecked = profanityChecked;
    }

    public boolean isImageResized() {
        return imageResized;
    }

    public void setImageResized(boolean imageResized) {
        this.imageResized = imageResized;
    }

    public boolean isSentimentAnalyzed() {
        return sentimentAnalyzed;
    }

    public void setSentimentAnalyzed(boolean sentimentAnalyzed) {
        this.sentimentAnalyzed = sentimentAnalyzed;
    }

    public boolean isReadyForPublishing() {
        return isActive && buyerChecked && profanityChecked && imageResized && sentimentAnalyzed;
    }
}

class Blackboard {
    private List<Message> messages = new ArrayList<>();

    public void addMessage(Message msg) {
        messages.add(msg);
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    public void removeMessage(Message msg) {
        messages.remove(msg);
    }
}

interface KnowledgeSource extends Comparable<KnowledgeSource> {
    void execute(Blackboard blackboard);
    int getPriority();

    @Override
    default int compareTo(KnowledgeSource other) {
        return Integer.compare(other.getPriority(), this.getPriority());
    }
}

class BuyerCheckKS implements KnowledgeSource {
    private Map<String, List<String>> productBuyers = new HashMap<>();

    public BuyerCheckKS() {
        productBuyers.put("Laptop", Arrays.asList("John", "Vasile", "Petrica"));
        productBuyers.put("Phone", Arrays.asList("Mary"));
        productBuyers.put("Book", Arrays.asList("Ann"));
    }

    @Override
    public void execute(Blackboard blackboard) {
        for (Message msg : blackboard.getMessages()) {
            if (!msg.isBuyerChecked() && msg.isActive()) {
                List<String> buyers = productBuyers.get(msg.getProduct());
                if (buyers == null || !buyers.contains(msg.getUsername())) {
                    msg.setActive(false);
                }
                msg.setBuyerChecked(true);
            }
        }
    }

    @Override
    public int getPriority() {
        return 1; // High priority for elimination
    }
}

class ProfanityCheckKS implements KnowledgeSource {
    @Override
    public void execute(Blackboard blackboard) {
        for (Message msg : blackboard.getMessages()) {
            if (msg.isActive() && !msg.isProfanityChecked()) {
                if (msg.getReviewText().contains("@#$%")) {
                    msg.setActive(false);
                }
                msg.setProfanityChecked(true);
            }
        }
    }

    @Override
    public int getPriority() {
        return 1;
    }
}

class ImageResizerKS implements KnowledgeSource {
    @Override
    public void execute(Blackboard blackboard) {
        for (Message msg : blackboard.getMessages()) {
            if (msg.isActive() && !msg.isImageResized()) {
                msg.setAttachment(msg.getAttachment().toLowerCase());
                msg.setImageResized(true);
            }
        }
    }

    @Override
    public int getPriority() {
        return 2; // Lower priority for transformations
    }
}

class SentimentAnalyzerKS implements KnowledgeSource {
    @Override
    public void execute(Blackboard blackboard) {
        for (Message msg : blackboard.getMessages()) {
            if (msg.isActive() && !msg.isSentimentAnalyzed()) {
                String text = msg.getReviewText();
                int upper = 0, lower = 0;
                for (char c : text.toCharArray()) {
                    if (Character.isUpperCase(c)) upper++;
                    else if (Character.isLowerCase(c)) lower++;
                }
                String suffix = "=";
                if (upper > lower) suffix = "+";
                else if (lower > upper) suffix = "-";
                msg.setReviewText(text + suffix);
                msg.setSentimentAnalyzed(true);
            }
        }
    }

    @Override
    public int getPriority() {
        return 2;
    }
}

class Controller {
    private Blackboard blackboard;
    private List<KnowledgeSource> knowledgeSources;

    public Controller(Blackboard blackboard, List<KnowledgeSource> knowledgeSources) {
        this.blackboard = blackboard;
        this.knowledgeSources = knowledgeSources;
        Collections.sort(this.knowledgeSources); // Sort by priority
    }

    public void process() {
        knowledgeSources.forEach(ks -> ks.execute(blackboard));
    }
}

// Main.java (Example Usage)
public class Main2 {
    public static void main(String[] args) {
        Blackboard blackboard = new Blackboard();
        blackboard.addMessage(new Message("John", "Laptop", "ok", "PICTURE"));
        blackboard.addMessage(new Message("Mary", "Phone", "@#$%)", "IMAGE"));
        blackboard.addMessage(new Message("Peter", "Phone", "GREAT", "ManyPictures"));
        blackboard.addMessage(new Message("Ann", "Book", "So GOOD", "Image"));

        List<KnowledgeSource> knowledgeSources = Arrays.asList(
            new BuyerCheckKS(),
            new ProfanityCheckKS(),
            new ImageResizerKS(),
            new SentimentAnalyzerKS()
        );

        Controller controller = new Controller(blackboard, knowledgeSources);
        controller.process();

        for (Message msg : blackboard.getMessages()) {
            if (msg.isActive()) {
                System.out.printf("%s, %s, %s, %s%n",
                    msg.getUsername(),
                    msg.getProduct(),
                    msg.getReviewText(),
                    msg.getAttachment());
            }
        }
    }
}

