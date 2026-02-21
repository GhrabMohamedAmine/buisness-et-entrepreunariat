package com.example.testp1;

import com.example.testp1.FinanceController;
import com.example.testp1.entities.Article;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.awt.Desktop;
import java.net.URI;

public class ArticleDetailController {

    @FXML private Label detailTitle;
    @FXML private Label detailSourceBadge;
    @FXML private Label detailDate;
    @FXML private Label articleContent;
    @FXML private Label breadcrumbSource;
    @FXML private ImageView articleImageView;
    @FXML private Hyperlink articleHyperlink;

    private String currentArticleUrl;

    /**
     * This is the "Entry Point" for data.
     * Called by the NewsCardController when a card is clicked.
     */
    public void setArticleData(Article article) {
        this.currentArticleUrl = article.url;

        // 1. Set Header & Breadcrumb Info
        detailTitle.setText(article.title);
        breadcrumbSource.setText(article.source != null ? article.source.name : "Market News");
        detailSourceBadge.setText(article.source != null ? article.source.name : "NEWS");

        // 2. Format Date (Simple String handling for now)
        if (article.publishedAt != null && article.publishedAt.length() >= 10) {
            detailDate.setText("PUBLISHED: " + article.publishedAt.substring(0, 10));
        }

        // 3. Set Body Content
        String fullText = "";
        if (article.content != null && !article.content.isEmpty()) {
            // Clean up the "[+XXX chars]" suffix if you want
            fullText = article.content.replaceAll("\\[\\+\\d+ chars\\]", "...");
        } else {
            fullText = article.description != null ? article.description : "No content available.";
        }
        articleContent.setText(fullText);

        // 4. Load Hero Image (Async to keep UI smooth)
        if (article.urlToImage != null && !article.urlToImage.isEmpty()) {
            // Using backgroundLoading = true
            Image heroImage = new Image(article.urlToImage, 800, 400, true, true, true);
            articleImageView.setImage(heroImage);
        }
    }

    @FXML
    private void handleBackToHub() {
        // Uses your existing scene switcher logic
        FinanceController.getInstance().loadView("Overviewpage.fxml");
    }

    @FXML
    private void handleOpenInBrowser() {
        if (currentArticleUrl != null) {
            try {
                // Opens the system's default browser (e.g., Chrome, Edge, Safari)
                Desktop.getDesktop().browse(new URI(currentArticleUrl));
            } catch (Exception e) {
                System.err.println("Error opening browser: " + e.getMessage());
            }
        }
    }
}