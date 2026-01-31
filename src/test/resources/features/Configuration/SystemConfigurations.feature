@role_edit
Feature: System configuration page - Tabs with defaults and update capabilities
  As a user
  I want to validate default state and update behavior of tabs under system configuration page
  So that configuration is correct and consitent across the application
 
  Scenario: Verify default state of System Information tab
    When the user navigates to "System Information" tab
    Then the user should be on the "System Information" tab
    Then the "System Information" tab displays following contols with default values
      | type     | label                                                | expected |
      | dropdown | Client Name                                          | Baush |
      | toggle   | Set as Default Sponsor?                              | On |
      | editbox  | Document Expiration (days)                           | 365 |
      | toggle   | Contact_Duplicate Check On/Off                       | On |
      | editbox  | Number of Characters to Check for Contact First Name | 4 |
      | toggle   | Institution_Duplicate Check On/Off                   | On |
      | editbox  | Number of Characters to Check for Institution Name   | 4 |
      | button   | Deploy                                               | disabled |
#@smoke
  Scenario: Check initial state of the List(s) tab when the user opens it
    When the user navigates to "List(s)" tab
    Then the user should be on the "List(s)" tab
    Then the "List(s)" tab displays following contols with default values
      | type      | label           | expected |
      | text      | List label      | List label |
      | searchbox | Filter Items... | Filter Items... |
      | linkText  | Country Name    | Country Name |
 
  Scenario Outline: Add a new active country
    When the user adds a country named "<country>" and activates it
    Then the country "<country>" appears in the list
 
    Examples:
      | country |
      | abc |