package Serial;

import java.util.*;

class Message {
    private String username;
    private String product;
    private String reviewText;
    private String attachment;

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
}

interface Filter {
    Message process(Message input);
}

class BuyerFilter implements Filter {
    private Map<String, List<String>> productBuyers = new HashMap<>();

    public BuyerFilter() {
        // Mock data: product -> list of buyers
        productBuyers.put("Laptop", Arrays.asList("John", "Vasile", "Petrica"));
        productBuyers.put("Phone", Arrays.asList("Mary"));
        productBuyers.put("Book", Arrays.asList("Ann"));
    }

    @Override
    public Message process(Message input) {
        List<String> buyers = productBuyers.get(input.getProduct());
        if (buyers == null || !buyers.contains(input.getUsername())) {
            return null; // Eliminate message
        }
        return input;
    }
}

class ProfanityFilter implements Filter {
    @Override
    public Message process(Message input) {
        if (input.getReviewText().contains("@#$%")) {
            return null; // Eliminate
        }
        return input;
    }
}

class ImageResizer implements Filter {
    @Override
    public Message process(Message input) {
        String attachment = input.getAttachment().toLowerCase();
        input.setAttachment(attachment);
        return input;
    }
}

class SentimentAnalyzer implements Filter {
    @Override
    public Message process(Message input) {
        String text = input.getReviewText();
        int upper = 0, lower = 0;
        for (char c : text.toCharArray()) {
            if (Character.isUpperCase(c)) upper++;
            else if (Character.isLowerCase(c)) lower++;
        }
        String suffix = "=";
        if (upper > lower) suffix = "+";
        else if (lower > upper) suffix = "-";
        input.setReviewText(text + suffix);
        return input;
    }
}

class Pipeline {
    private List<Filter> filters;

    public Pipeline(List<Filter> filters) {
        this.filters = filters;
    }

    public Message process(Message input) {
        for (Filter filter : filters) {
            input = filter.process(input);
            if (input == null) break;
        }
        return input;
    }
}

// Pipes and filters
public class Main {
    public static void main(String[] args) {
        List<Filter> filters = Arrays.asList(
            new BuyerFilter(),
            new ProfanityFilter(),
            new ImageResizer(),
            new SentimentAnalyzer()
        );
        Pipeline pipeline = new Pipeline(filters);

        List<Message> inputMessages = Arrays.asList(
            new Message("John", "Laptop", "ok", "PICTURE"),
            new Message("Mary", "Phone", "@#$%)", "IMAGE"),
            new Message("Peter", "Phone", "GREAT", "ManyPictures"),
            new Message("Ann", "Book", "So GOOD", "Image")
        );

        for (Message msg : inputMessages) {
            Message result = pipeline.process(msg);
            if (result != null) {
                System.out.printf("%s, %s, %s, %s%n",
                    result.getUsername(),
                    result.getProduct(),
                    result.getReviewText(),
                    result.getAttachment());
            }
        }
    }
}


