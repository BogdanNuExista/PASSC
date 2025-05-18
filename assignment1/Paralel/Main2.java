package Paralel;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.io.*;

class Message {
    private String username;
    private String productName;
    private String reviewText;
    private String attachment;
    private volatile boolean valid = true;
    private Set<String> completedTasks = ConcurrentHashMap.newKeySet();

    public Message(String username, String productName, String reviewText, String attachment) {
        this.username = username;
        this.productName = productName;
        this.reviewText = reviewText;
        this.attachment = attachment;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getUsername() {
        return username;
    }

    public String getProductName() {
        return productName;
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

    public boolean isEligibleFor(String taskName, List<String> prerequisites) {
        if (!valid || completedTasks.contains(taskName)) return false;
        for (String prereq : prerequisites) {
            if (!completedTasks.contains(prereq)) return false;
        }
        return true;
    }

    public void addCompletedTask(String taskName) {
        completedTasks.add(taskName);
    }

    public boolean allTasksCompleted(List<String> requiredTasks) {
        return completedTasks.containsAll(requiredTasks);
    }

    public String toCsv() {
        return String.join(", ", username, productName, reviewText, attachment);
    }
}

//////////////////////////////////////////////

class Blackboard {
    private CopyOnWriteArrayList<Message> messages = new CopyOnWriteArrayList<>();

    public void addMessage(Message message) {
        messages.add(message);
    }

    public CopyOnWriteArrayList<Message> getMessages() {
        return messages;
    }
}

abstract class KnowledgeSource implements Runnable {
    protected Blackboard blackboard;
    protected String taskName;
    protected List<String> prerequisites;

    public KnowledgeSource(Blackboard blackboard, String taskName, List<String> prerequisites) {
        this.blackboard = blackboard;
        this.taskName = taskName;
        this.prerequisites = prerequisites;
    }

    protected abstract void processMessage(Message message);

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            for (Message message : blackboard.getMessages()) {
                synchronized (message) {
                    if (message.isEligibleFor(taskName, prerequisites)) {
                        processMessage(message);
                        message.addCompletedTask(taskName);
                    }
                }
            }
            try {
                Thread.sleep(10); // nu vrem sa accesam prea des
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

class CheckProfanityKS extends KnowledgeSource {
    public CheckProfanityKS(Blackboard blackboard) {
        super(blackboard, "CheckProfanity", Collections.emptyList());
    }

    @Override
    protected void processMessage(Message message) {
        String review = message.getReviewText();
        if (review.contains("@") || review.contains("#") || review.contains("$") || review.contains("%")) {
            message.setValid(false);
        }
    }
}

class CheckBuyerKS extends KnowledgeSource {
    private Map<String, List<String>> buyers = Map.of(
        "Laptop", List.of("John"),
        "Phone", List.of("Mary"),
        "Book", List.of("Ann","Bobo","BoboLast")
    );

    public CheckBuyerKS(Blackboard blackboard) {
        super(blackboard, "CheckBuyer", Collections.emptyList());
    }

    @Override
    protected void processMessage(Message message) {
        String product = message.getProductName();
        String user = message.getUsername();
        List<String> productBuyers = buyers.get(product);
        if (productBuyers == null || !productBuyers.contains(user)) {
            message.setValid(false);
        }
    }
}

class ResizeImagesKS extends KnowledgeSource {
    public ResizeImagesKS(Blackboard blackboard) {
        super(blackboard, "ResizeImages", List.of("CheckProfanity", "CheckBuyer"));
    }

    @Override
    protected void processMessage(Message message) {
        String attachment = message.getAttachment();
        message.setAttachment(attachment.toLowerCase());
    }
}

class SentimentDetectionKS extends KnowledgeSource {
    public SentimentDetectionKS(Blackboard blackboard) {
        super(blackboard, "SentimentDetection", List.of("CheckProfanity", "CheckBuyer"));
    }

    @Override
    protected void processMessage(Message message) {
        String text = message.getReviewText();
        int upper = 0, lower = 0;
        for (char c : text.toCharArray()) {
            if (Character.isUpperCase(c)) upper++;
            else if (Character.isLowerCase(c)) lower++;
        }
        if (upper > lower) message.setReviewText(text + "+");
        else if (lower > upper) message.setReviewText(text + "-");
        else message.setReviewText(text + "=");
    }
}

class MessageSource implements Runnable {
    private Blackboard blackboard;
    private List<String> inputLines;

    public MessageSource(Blackboard blackboard, List<String> inputLines) {
        this.blackboard = blackboard;
        this.inputLines = inputLines;
    }

    @Override
    public void run() {
        for (String line : inputLines) {
            String[] parts = line.split(", ");
            Message message = new Message(parts[0], parts[1], parts[2], parts[3]);
            blackboard.addMessage(message);
        }
    }
}

class MessageOutput implements Runnable {
    private Blackboard blackboard;
    private List<String> requiredTasks;

    public MessageOutput(Blackboard blackboard, List<String> requiredTasks) {
        this.blackboard = blackboard;
        this.requiredTasks = requiredTasks;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            for (Message message : blackboard.getMessages()) {
                synchronized (message) {
                    if (message.isValid() && message.allTasksCompleted(requiredTasks)) {
                        System.out.println(message.toCsv());
                        blackboard.getMessages().remove(message);
                    }
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

/// adding political + rmv links filters

// Political Propaganda Knowledge Source
class CheckPoliticalPropagandaKS extends KnowledgeSource {
    public CheckPoliticalPropagandaKS(Blackboard blackboard) {
        super(blackboard, "CheckPoliticalPropaganda", Collections.emptyList());
    }

    @Override
    protected void processMessage(Message message) {
        String review = message.getReviewText();
        if (review.contains("+++") || review.contains("--")) {
            message.setValid(false);
        }
    }
}

// Competitor Links Knowledge Source
class RemoveLinksKS extends KnowledgeSource {
    public RemoveLinksKS(Blackboard blackboard) {
        super(blackboard, "RemoveLinks", List.of("CheckProfanity", "CheckBuyer", "CheckPoliticalPropaganda"));
    }

    @Override
    protected void processMessage(Message message) {
        String cleaned = message.getReviewText().replace("http", "");
        message.setReviewText(cleaned);
    }
}

public class Main2 {
    public static void main(String[] args) {
        Blackboard blackboard = new Blackboard();

        // Read input lines from input2.txt
        List<String> inputLines = new ArrayList<>();
        try {
            inputLines = Files.readAllLines(Paths.get("input2.txt"));
        } catch (IOException e) {
            System.err.println("Error reading input file: " + e.getMessage());
            return;
        }

        List<String> requiredTasks = List.of(
            "CheckProfanity", 
            "CheckBuyer",
            "CheckPoliticalPropaganda",
            "RemoveLinks",
            "ResizeImages", 
            "SentimentDetection",
            "CheckPoliticalPropaganda",
            "RemoveLinks"
        );

        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(new MessageSource(blackboard, inputLines));
        executor.execute(new CheckProfanityKS(blackboard));
        executor.execute(new CheckBuyerKS(blackboard));
        executor.execute(new ResizeImagesKS(blackboard));
        executor.execute(new SentimentDetectionKS(blackboard));
        executor.execute(new CheckPoliticalPropagandaKS(blackboard));
        executor.execute(new RemoveLinksKS(blackboard));
        executor.execute(new MessageOutput(blackboard, requiredTasks));

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) { // 5 seconds timeout
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}