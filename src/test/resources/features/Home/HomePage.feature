Feature: Home page features including company logo, main navigation links, bell icon and user icon.
  As an authenticated user
  I want to use home page to access core modules
  So that I can quickly access main navigation links, bell icon for messages and user icon to get user action
  functionality.

  Scenario: Navigate to homepage via company logo
    Given the user is on "<page>" page
    Then the company logo "<companyName>" is visible on the top navigation bar
    When the user navigates to the homepage using the company logo

    Examples:
      | page          | companyName |
      | Configuration | Sitero      |
