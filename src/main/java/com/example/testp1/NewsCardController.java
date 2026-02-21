package com.example.testp1;

import com.example.testp1.entities.Article;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class NewsCardController {

    @FXML private HBox newsCardRoot;
    @FXML private ImageView thumbnailView;
    @FXML private Label sourceLabel;
    @FXML private Label titleLabel;
    @FXML private Label dateLabel;

    private Article article;

    public void setArticleData(Article article) {
        this.article = article;

        titleLabel.setText(article.title);
        sourceLabel.setText(article.source != null ? article.source.name : "News");

        // Handling the date string from the API (2026-02-21T...)
        if (article.publishedAt != null && article.publishedAt.length() >= 10) {
            dateLabel.setText(article.publishedAt.substring(0, 10));
        }

        // Background loading for the image to prevent UI stutter
        if (article.urlToImage != null && !article.urlToImage.isEmpty()) {
            Image img = new Image(article.urlToImage, 120, 90, true, true, true);
            thumbnailView.setImage(img);
        } else {
            // Optional: Set a local placeholder image if url is null
            // thumbnailView.setImage(new Image(getClass().getResourceAsStream("/images/news-placeholder.png")));
        }
    }

    @FXML
    private void handleCardClicked() {
        System.out.println("Transitioning to Reader Mode: " + article.title);

        // 1. Call your new specialized function
        // We explicitly tell it we expect an ArticleDetailController
        ArticleDetailController detailCtrl = FinanceController.getInstance().loadViewAPI("ArticleDetails.fxml");

        // 2. Now you can use the controller because it's no longer void!
        if (detailCtrl != null) {
            detailCtrl.setArticleData(this.article);
        } else {
            System.err.println("Error: ArticleDetailController could not be initialized.");
        }
    }
}