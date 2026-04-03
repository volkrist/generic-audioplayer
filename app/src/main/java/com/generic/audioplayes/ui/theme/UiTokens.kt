package com.generic.audioplayes.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralized spacing, radii, and chrome sizes for Home shell, lists, grids, Now Playing, and mini player.
 * Prefer these over raw `.dp` literals in UI code.
 */
object UiTokens {
    // --- Corners ---
    val cornerLarge = 18.dp
    /** Playlist tiles, some cards, album grid art. */
    val cornerExtraLarge = 20.dp
    val cornerMedium = 14.dp
    val cornerSmall = 10.dp
    val cornerXs = 8.dp
    /** Ripple bounds for ~44dp touch targets. */
    val cornerPill = 22.dp

    /** Thin progress under mini player row in [HomeFragment] peek — defined early for [miniPlayerPeekHeight]. */
    val progressBarThin = 3.dp

    // --- Bars & list rows ---
    val topBarHeight = 56.dp
    /** Matches [HomeBottomBar] (taller tonal nav). */
    val bottomBarHeight = 92.dp
    val bottomNavHeight = 92.dp
    /**
     * Inner [MiniPlayer] row [Modifier.height] (art + text + play control).
     * Vertical padding is applied after height — total row block = [miniPlayerRowHeight] + 2×[miniPlayerVerticalPadding].
     */
    val miniPlayerRowHeight = 88.dp
    val miniPlayerVerticalPadding = 10.dp
    /** Row block + [progressBarThin] — must match [MiniPlayer] + peek [Column] in [HomeFragment]. */
    val miniPlayerPeekHeight = miniPlayerRowHeight + miniPlayerVerticalPadding * 2 + progressBarThin

    val listItemHeightCompact = 64.dp
    val listItemHeightStandard = 70.dp
    val listItemHeightTall = 80.dp
    /** Home → Songs flat row (reference list density). */
    val listItemHeightSongsHome = 76.dp

    // --- Artwork ---
    val artworkMini = 48.dp
    val artworkList = 48.dp
    val artworkPlaylistHero = 50.dp
    val artworkMedium = 56.dp
    val artworkNowPlayingPlaceholder = 96.dp
    val artworkThumbSmall = 20.dp
    val artworkThumbMini = 26.dp
    /** Reference max width for portrait NP art (used with fraction). */
    val artworkNowPlayingMaxPortrait = 240.dp

    // --- Padding ---
    val paddingScreen = 16.dp
    val paddingSection = 12.dp
    val paddingItem = 8.dp
    val paddingItemTight = 4.dp
    val paddingCard = 12.dp
    val paddingHorizontalComfort = 10.dp
    val paddingSheetHorizontal = 24.dp
    val paddingSheetBottom = 24.dp

    // --- Grid ---
    val gridMinCellSize = 158.dp
    val gridSpacing = 10.dp
    val gridSpacingTight = 8.dp
    val gridCardMaxWidth = 220.dp
    /** Horizontal song card in grid (SongCardV3). */
    val songCardGridMaxWidth = 200.dp
    val gridVerticalSpacing = 6.dp
    val gridContentPaddingTop = 8.dp
    val gridContentPaddingBottom = 28.dp
    val smartSectionTitlePaddingBottom = 4.dp
    val userPlaylistSectionTitlePaddingTop = 12.dp
    val playlistTileVerticalPadding = 4.dp

    // --- Icons & controls ---
    val iconSizeSmall = 24.dp
    val iconSizeMedium = 34.dp
    /** Speed/pitch launcher, secondary prominent glyph. */
    val iconSizeProminent = 28.dp
    val iconSizeTouch = 44.dp
    /** Smart playlist / create-tile icons inside square gradient cells ([Playlists.kt]). */
    val smartTileIcon = 24.dp
    /** Left artwork on playlist list rows (reference ~56–64dp). */
    val playlistListRowArt = 56.dp
    val iconSizeLarge = 50.dp
    val playControlSize = 70.dp
    val rippleSmall = 20.dp
    val rippleMedium = 22.dp
    val rippleLarge = 25.dp
    val rippleHuge = 35.dp

    // --- Scaffold / sheets / progress ---
    /** Bottom nav ([bottomNavHeight]) + comfortable gap above gesture bar — idle library inset. */
    val scaffoldBottomPaddingIdle = 104.dp
    val sheetCornerTopLarge = 30.dp
    val sheetCornerTopSmall = 16.dp
    val elevationNone = 0.dp
    /** Blended with [LocalAbsoluteTonalElevation] for scaffold surfaces. */
    val elevationTonalBlend = 3.dp
    val elevationTonalLow = 2.dp
    val elevationSurface = 6.dp
    val elevationNowPlayingArt = 20.dp
    val actionSheetMaxHeight = 520.dp

    // --- Play / shuffle strip ---
    val playShuffleCardHeight = 92.dp
    val playShuffleButtonHeight = 52.dp
    val playShuffleCorner = 16.dp

    // --- Density helpers ---
    val listContentSpacing = 6.dp
    val rowSpacingComfort = 10.dp
    val metaSpacingSmall = 6.dp
    /** Tight gap between title lines on playlist tiles. */
    val textLineGapTight = 2.dp

    val sleepTimerFieldWidth = 80.dp
    val sleepTimerSeparatorWidth = 12.dp

    // --- Queue sheet chrome ---
    val queueSheetDragHandleWidth = 40.dp
    val queueSheetDragHandleHeight = 4.dp
    val queueSheetDragHandleCorner = 2.dp
    val queueDividerHorizontalPadding = paddingSheetHorizontal
    val queueDividerVerticalPadding = paddingItemTight

    // --- Now Playing / MusicSlider ---
    val musicSliderTimeLabelSp = 14.sp
    /** Horizontal inset for full player so content stays inside safe drawing area. */
    val nowPlayingScreenHorizontalPadding = 20.dp

    /**
     * Max alpha for the library scrim behind the expanded player (multiplied by full-player fraction).
     * See [com.generic.audioplayes.home.HomeFragment].
     */
    const val libraryBehindPlayerDimAlpha = 0.18f

    /** Mini strip scale when full player is open (peek hidden); 1f when mini is fully visible. */
    const val miniPlayerPeekScaleCollapsed = 0.94f
    const val miniPlayerPeekScaleExpanded = 1f
}
