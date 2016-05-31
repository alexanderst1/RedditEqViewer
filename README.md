# Capstone-Project "Eqv Viewer for Reddit"

Reddit (http://reddit.com) is an entertainment and social networking site where community members can create posts and submit links in themed areas called "subreddits." The following exemplifies how you might address the task of creating a Reddit viewing app in the context of the Android Fundamentals project specifications.

###### Problem:

The user subscribes to a large number of subreddits, finds it hard to keep up with them, and has long stretches where some are neglected. He/she wants a way to get news/posts/threads from all of them equally.

###### Proposed Solution:

Design an app that allows a user to indicate a set of subreddits that they would like to follow and store. Your app should present them with a post from one of their selected subreddits. They can interact with the post (i.e. view image posts, play video posts, etc) or they can dismiss it to receive another. You should display comments (or a subset), thumbnails, and comment text and links. You should use intents to communicate with the Android apps, components, and services that you don’t want to or cannot implement. For instance, to view a link, you could use a WebView or you could send an Intent to the browser.

#### Project design

[Capstone_Stage1.pdf](img/Capstone_Stage1.pdf?raw=true)

#### Project implementation screenshots

###### Subreddits with top links & Link with image and comments

![Subreddits with top links](img/01_subreddits_with_top_links.png?raw=true "Subreddits with top links")
![Link with image and comments](img/02_link_with_immage_and_comments.png?raw=true "Link with image and comments")

###### Link with comments (no image) & Link open in internal browser

![Link with comments (no image)](img/03_link_with_comments.png?raw=true "Link with comments (no image)")
![Link open in internal browser](img/04_internal_browser.png?raw=true "Link open in internal browser")

###### Subscription & Subreddit search

![Subscription](img/05_subscription.png?raw=true "Subscription")
![Subreddit search](img/06_search.png?raw=true "Subreddit search")

###### Account selection & OAuth2 authorization

![Account selection](img/07_choose_an_account.png?raw=true "Account selection")
![OAuth2 authorization](img/08_authorization_1.png?raw=true "OAuth2 authorization")

###### OAuth2 authorization (confirmation)

![OAuth2 authorization (confirmation)](img/09_authorization_2.png?raw=true "OAuth2 authorization (confirmation)")
