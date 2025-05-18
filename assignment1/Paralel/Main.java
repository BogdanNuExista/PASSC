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
    private boolean valid = true;
    private boolean poison = false;

    public Message(String username, String productName, String reviewText, String attachment) {
        this.username = username;
        this.productName = productName;
        this.reviewText = reviewText;
        this.attachment = attachment;
    }

    public static Message createPoison() { // for ending the program
        Message m = new Message("", "", "", "");
        m.poison = true;
        return m;
    }

    public boolean isPoison() {
        return poison;
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

    public String toCsv() {
        return String.join(", ", username, productName, reviewText, attachment);
    }
}

class MessageSource implements Runnable {
    private final BlockingQueue<Message> outputQueue;
    private final List<String> inputLines;

    public MessageSource(BlockingQueue<Message> output, List<String> inputs) {
        this.outputQueue = output;
        this.inputLines = inputs;
    }

    @Override
    public void run() {
        try {
            for (String line : inputLines) {
                String[] parts = line.split(", ");
                Message message = new Message(parts[0], parts[1], parts[2], parts[3]);
                outputQueue.put(message);
            }
            outputQueue.put(Message.createPoison());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class CheckProfanityFilter implements Runnable {
    private final BlockingQueue<Message> inputQueue;
    private final BlockingQueue<Message> outputQueue;

    public CheckProfanityFilter(BlockingQueue<Message> input, BlockingQueue<Message> output) {
        this.inputQueue = input;
        this.outputQueue = output;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message message = inputQueue.take();
                if (message.isPoison()) {
                    outputQueue.put(message);
                    break;
                }
                if (message.isValid()) {
                    String review = message.getReviewText();
                    if (review.contains("@") || review.contains("#") || review.contains("$") || review.contains("%")) {
                        message.setValid(false);
                    }
                }
                outputQueue.put(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class CheckBuyerFilter implements Runnable {
    private final BlockingQueue<Message> inputQueue;
    private final BlockingQueue<Message> outputQueue;
    private final Map<String, List<String>> buyers;

    public CheckBuyerFilter(BlockingQueue<Message> input, BlockingQueue<Message> output) {
        this.inputQueue = input;
        this.outputQueue = output;
        buyers = new HashMap<>();
        buyers.put("Laptop", Arrays.asList("John"));
        buyers.put("Phone", Arrays.asList("Mary"));
        buyers.put("Book", Arrays.asList("Ann", "Bobo","BoboLast"));
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message message = inputQueue.take();
                if (message.isPoison()) {
                    outputQueue.put(message);
                    break;
                }
                if (message.isValid()) {
                    String product = message.getProductName();
                    String user = message.getUsername();
                    List<String> productBuyers = buyers.get(product);
                    if (productBuyers == null || !productBuyers.contains(user)) {
                        message.setValid(false);
                    }
                }
                outputQueue.put(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class ResizeImagesFilter implements Runnable {
    private final BlockingQueue<Message> inputQueue;
    private final BlockingQueue<Message> outputQueue;

    public ResizeImagesFilter(BlockingQueue<Message> input, BlockingQueue<Message> output) {
        this.inputQueue = input;
        this.outputQueue = output;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message message = inputQueue.take();
                if (message.isPoison()) {
                    outputQueue.put(message);
                    break;
                }
                if (message.isValid()) {
                    String attachment = message.getAttachment();
                    message.setAttachment(attachment.toLowerCase());
                }
                outputQueue.put(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class SentimentDetectionFilter implements Runnable {
    private final BlockingQueue<Message> inputQueue;
    private final BlockingQueue<Message> outputQueue;

    public SentimentDetectionFilter(BlockingQueue<Message> input, BlockingQueue<Message> output) {
        this.inputQueue = input;
        this.outputQueue = output;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message message = inputQueue.take();
                if (message.isPoison()) {
                    outputQueue.put(message);
                    break;
                }
                if (message.isValid()) {
                    String text = message.getReviewText();
                    int upper = 0, lower = 0;
                    for (char c : text.toCharArray()) {
                        if (Character.isUpperCase(c)) {
                            upper++;
                        } else if (Character.isLowerCase(c)) {
                            lower++;
                        }
                    }
                    if (upper > lower) {
                        message.setReviewText(text + "+");
                    } else if (lower > upper) {
                        message.setReviewText(text + "-");
                    } else {
                        message.setReviewText(text + "=");
                    }
                }
                outputQueue.put(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class MessageOutput implements Runnable {
    private final BlockingQueue<Message> inputQueue;

    public MessageOutput(BlockingQueue<Message> input) {
        this.inputQueue = input;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message message = inputQueue.take();
                if (message.isPoison()) {
                    break;
                }
                if (message.isValid()) {
                    System.out.println(message.toCsv());
                    
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/// adding political + remove links filter

// Political Propaganda Filter
class CheckPoliticalPropagandaFilter implements Runnable {
    private final BlockingQueue<Message> inputQueue;
    private final BlockingQueue<Message> outputQueue;

    public CheckPoliticalPropagandaFilter(BlockingQueue<Message> input, BlockingQueue<Message> output) {
        this.inputQueue = input;
        this.outputQueue = output;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message message = inputQueue.take();
                if (message.isPoison()) {
                    outputQueue.put(message);
                    break;
                }
                if (message.isValid() && 
                    (message.getReviewText().contains("+++") || message.getReviewText().contains("--"))) {
                    message.setValid(false);
                }
                outputQueue.put(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// Competitor Links Filter
class RemoveLinksFilter implements Runnable {
    private final BlockingQueue<Message> inputQueue;
    private final BlockingQueue<Message> outputQueue;

    public RemoveLinksFilter(BlockingQueue<Message> input, BlockingQueue<Message> output) {
        this.inputQueue = input;
        this.outputQueue = output;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message message = inputQueue.take();
                if (message.isPoison()) {
                    outputQueue.put(message);
                    break;
                }
                if (message.isValid()) {
                    String cleaned = message.getReviewText().replace("http", "");
                    message.setReviewText(cleaned);
                }
                outputQueue.put(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

public class Main {
    public static void main(String[] args) {
        BlockingQueue<Message> queue1 = new LinkedBlockingQueue<>();
        BlockingQueue<Message> queue2 = new LinkedBlockingQueue<>();
        BlockingQueue<Message> queue3 = new LinkedBlockingQueue<>();
        BlockingQueue<Message> queue4 = new LinkedBlockingQueue<>();
        BlockingQueue<Message> queue5 = new LinkedBlockingQueue<>();
        BlockingQueue<Message> queue6 = new LinkedBlockingQueue<>();
        BlockingQueue<Message> queue7 = new LinkedBlockingQueue<>();

        // Read input lines from input.txt
        List<String> inputLines = new ArrayList<>();
        try {
            inputLines = Files.readAllLines(Paths.get("input.txt"));
        } catch (IOException e) {
            System.err.println("Error reading input file: " + e.getMessage());
            return;
        }

        ExecutorService executor = Executors.newCachedThreadPool();

        executor.execute(new MessageSource(queue1, inputLines));
        executor.execute(new CheckProfanityFilter(queue1, queue2));
        executor.execute(new CheckBuyerFilter(queue2, queue3));
        executor.execute(new CheckPoliticalPropagandaFilter(queue3, queue4));
        executor.execute(new RemoveLinksFilter(queue4, queue5));
        executor.execute(new ResizeImagesFilter(queue5, queue6));
        executor.execute(new SentimentDetectionFilter(queue6, queue7));
        executor.execute(new MessageOutput(queue7));

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}