# Graph Report - minebucks  (2026-05-08)

## Corpus Check
- 37 files · ~192,464 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 181 nodes · 184 edges · 27 communities (14 shown, 13 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 12 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `898d5bbb`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]

## God Nodes (most connected - your core abstractions)
1. `RevenueViewModel` - 15 edges
2. `MineBucks` - 10 edges
3. `RevenueDao` - 9 edges
4. `DataStoreManager` - 9 edges
5. `DashboardScreen()` - 8 edges
6. `ModrinthService` - 6 edges
7. `RevenueRepository` - 6 edges
8. `Privacy Policy for MineBucks` - 6 edges
9. `AdManager` - 5 edges
10. `RevenueWidget` - 5 edges

## Surprising Connections (you probably didn't know these)
- `DashboardScreen()` --calls--> `RevenueHistoryGraph()`  [INFERRED]
  app/src/main/java/com/Sourish25/MineBucks/ui/DashboardScreen.kt → app/src/main/java/com/Sourish25/MineBucks/ui/RevenueHistoryGraph.kt
- `DashboardScreen()` --calls--> `SupportUsButton()`  [INFERRED]
  app/src/main/java/com/Sourish25/MineBucks/ui/DashboardScreen.kt → app/src/main/java/com/Sourish25/MineBucks/ui/SupportUsButton.kt
- `DashboardScreen()` --calls--> `HistoryItem()`  [INFERRED]
  app/src/main/java/com/Sourish25/MineBucks/ui/DashboardScreen.kt → app/src/main/java/com/Sourish25/MineBucks/ui/RevenueHistoryList.kt
- `HistoryItem()` --calls--> `formatCurrency()`  [INFERRED]
  app/src/main/java/com/Sourish25/MineBucks/ui/RevenueHistoryList.kt → app/src/main/java/com/Sourish25/MineBucks/ui/DashboardScreen.kt
- `SettingsScreen()` --calls--> `CurrencyOption`  [INFERRED]
  app/src/main/java/com/Sourish25/MineBucks/ui/SettingsScreen.kt → app/src/main/java/com/Sourish25/MineBucks/ui/OnboardingScreen.kt

## Communities (27 total, 13 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.09
Nodes (22): 1. Project Origins & The Problem, 2.1. The Modrinth API Dilemma, 2.2. The CurseForge "Black Box", 2. Engineering Journey & Technical Challenges, 3. Other Implemented Features, 4. Usage Guide (Step-by-Step), 5. Future Roadmap, 6. License (+14 more)

### Community 1 - "Community 1"
Cohesion: 0.15
Nodes (5): create(), DailyRevenue, RevenueUiState, RevenueViewModel, SettingsData

### Community 2 - "Community 2"
Cohesion: 0.17
Nodes (8): MainActivity, MineBucksTheme(), BrowserTab, CurseForgeLoginScreen(), CurrencyOption, OnboardingScreen(), SettingsScreen(), ThemeSelectionBar()

### Community 3 - "Community 3"
Cohesion: 0.23
Nodes (10): DashboardScreen(), formatCurrency(), GreetingSection(), PlatformCard(), TotalRevenueCard(), WavyGraphCanvas(), RevenueHistoryGraph(), HistoryItem() (+2 more)

### Community 4 - "Community 4"
Cohesion: 0.15
Nodes (3): RealRevenueDataSource, RevenueRepository, RevenueResult

### Community 5 - "Community 5"
Cohesion: 0.18
Nodes (3): CurrencyService, ModrinthService, ModrinthServiceV3

### Community 8 - "Community 8"
Cohesion: 0.22
Nodes (8): CurrencyResponse, ModrinthAnalytics, ModrinthPayout, ModrinthPayoutData, ModrinthPayoutHistory, ModrinthProject, ModrinthRevenueAnalyticsResponse, ModrinthUser

### Community 10 - "Community 10"
Cohesion: 0.29
Nodes (6): 1. Overview, 2. Information We Collect, 3. Third-Party Services (AdMob), 4. Children’s Privacy, 5. Contact Us, Privacy Policy for MineBucks

## Knowledge Gaps
- **38 isolated node(s):** `ModrinthUser`, `ModrinthPayoutData`, `ModrinthProject`, `ModrinthAnalytics`, `ModrinthRevenueAnalyticsResponse` (+33 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **13 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DashboardScreen()` connect `Community 3` to `Community 2`?**
  _High betweenness centrality (0.016) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `DashboardScreen()` (e.g. with `.onCreate()` and `RevenueHistoryGraph()`) actually correct?**
  _`DashboardScreen()` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `ModrinthUser`, `ModrinthPayoutData`, `ModrinthProject` to the rest of the system?**
  _38 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.09 - nodes in this community are weakly interconnected._