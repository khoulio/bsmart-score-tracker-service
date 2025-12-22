/**
 * BSmart Score Tracker - Live Refresh Script
 * Auto-refreshes match data for live matches
 */

(function() {
    'use strict';

    // Configuration
    const REFRESH_INTERVAL = 10000; // 10 seconds for live matches
    const INDICATOR_DISPLAY_TIME = 1000; // 1 second

    // State
    let refreshTimer = null;
    let isAutoRefreshEnabled = false;

    /**
     * Initialize auto-refresh on page load
     */
    document.addEventListener('DOMContentLoaded', function() {
        // Check if we're on a page that should auto-refresh
        const shouldAutoRefresh = document.body.dataset.autoRefresh === 'true';

        if (shouldAutoRefresh) {
            enableAutoRefresh();
        }

        // Check if current page has live matches
        checkForLiveMatches();
    });

    /**
     * Check if there are any live matches on the current page
     */
    function checkForLiveMatches() {
        const liveMatches = document.querySelectorAll('.match-card-live');

        if (liveMatches.length > 0) {
            enableAutoRefresh();
        }
    }

    /**
     * Enable auto-refresh
     */
    function enableAutoRefresh() {
        if (isAutoRefreshEnabled) return;

        isAutoRefreshEnabled = true;
        console.log('[Auto-Refresh] Enabled with interval:', REFRESH_INTERVAL, 'ms');

        // Start refresh timer
        refreshTimer = setInterval(refreshPage, REFRESH_INTERVAL);

        // Show indicator
        showRefreshIndicator();
    }

    /**
     * Disable auto-refresh
     */
    function disableAutoRefresh() {
        if (!isAutoRefreshEnabled) return;

        isAutoRefreshEnabled = false;
        console.log('[Auto-Refresh] Disabled');

        // Clear timer
        if (refreshTimer) {
            clearInterval(refreshTimer);
            refreshTimer = null;
        }

        // Hide indicator
        hideRefreshIndicator();
    }

    /**
     * Refresh the current page
     */
    function refreshPage() {
        console.log('[Auto-Refresh] Refreshing page...');

        // Show refresh indicator
        flashRefreshIndicator();

        // Reload page
        location.reload();
    }

    /**
     * Show refresh indicator
     */
    function showRefreshIndicator() {
        let indicator = document.getElementById('autoRefreshIndicator');

        if (!indicator) {
            indicator = createRefreshIndicator();
            document.body.appendChild(indicator);
        }

        indicator.classList.add('active');
    }

    /**
     * Hide refresh indicator
     */
    function hideRefreshIndicator() {
        const indicator = document.getElementById('autoRefreshIndicator');

        if (indicator) {
            indicator.classList.remove('active');
        }
    }

    /**
     * Flash refresh indicator
     */
    function flashRefreshIndicator() {
        const indicator = document.getElementById('autoRefreshIndicator');

        if (indicator) {
            indicator.style.opacity = '1';
            setTimeout(() => {
                indicator.style.opacity = '0.8';
            }, INDICATOR_DISPLAY_TIME);
        }
    }

    /**
     * Create refresh indicator element
     */
    function createRefreshIndicator() {
        const indicator = document.createElement('div');
        indicator.id = 'autoRefreshIndicator';
        indicator.className = 'auto-refresh-indicator';
        indicator.innerHTML = '<i class="bi bi-arrow-clockwise me-2"></i>Auto-refresh actif';
        indicator.style.opacity = '0.8';

        // Add click handler to disable
        indicator.addEventListener('click', function() {
            if (confirm('Désactiver le rafraîchissement automatique ?')) {
                disableAutoRefresh();
            }
        });

        indicator.style.cursor = 'pointer';
        indicator.title = 'Cliquer pour désactiver';

        return indicator;
    }

    /**
     * Ajax refresh for specific match cards (alternative to full page reload)
     * Can be used for smoother updates without full page reload
     */
    function refreshMatchCard(matchId) {
        fetch(`/api/matches/${matchId}`)
            .then(response => response.json())
            .then(data => {
                updateMatchCard(matchId, data);
            })
            .catch(error => {
                console.error('[Auto-Refresh] Error refreshing match:', matchId, error);
            });
    }

    /**
     * Update match card with new data
     */
    function updateMatchCard(matchId, matchData) {
        const card = document.querySelector(`[data-match-id="${matchId}"]`);

        if (!card) return;

        // Update score
        const scoreHome = card.querySelector('.score-home');
        const scoreAway = card.querySelector('.score-away');

        if (scoreHome) scoreHome.textContent = matchData.scoreHome || 0;
        if (scoreAway) scoreAway.textContent = matchData.scoreAway || 0;

        // Update minute
        const minute = card.querySelector('.match-minute');
        if (minute && matchData.minute) {
            minute.textContent = matchData.minute + "'";
        }

        // Update status
        const statusBadge = card.querySelector('.badge-match-status');
        if (statusBadge) {
            statusBadge.className = 'badge badge-match-' + matchData.status.toLowerCase();
            statusBadge.textContent = formatStatus(matchData.status);
        }

        // Add flash effect
        card.style.transition = 'background-color 0.3s';
        card.style.backgroundColor = 'rgba(13, 110, 253, 0.1)';
        setTimeout(() => {
            card.style.backgroundColor = '';
        }, 300);
    }

    /**
     * Format match status for display
     */
    function formatStatus(status) {
        const statusMap = {
            'SCHEDULED': 'Programmé',
            'IN_PLAY': 'En direct',
            'HALF_TIME': 'Mi-temps',
            'FINISHED': 'Terminé'
        };

        return statusMap[status] || status;
    }

    /**
     * Refresh all live matches on the page (Ajax alternative)
     */
    function refreshAllLiveMatches() {
        const liveMatches = document.querySelectorAll('.match-card-live');

        liveMatches.forEach(card => {
            const matchId = card.dataset.matchId;
            if (matchId) {
                refreshMatchCard(matchId);
            }
        });
    }

    // Expose functions globally for manual control
    window.ScoreTracker = {
        enableAutoRefresh: enableAutoRefresh,
        disableAutoRefresh: disableAutoRefresh,
        refreshPage: refreshPage,
        refreshAllLiveMatches: refreshAllLiveMatches
    };

    // Handle page visibility changes
    document.addEventListener('visibilitychange', function() {
        if (document.hidden) {
            console.log('[Auto-Refresh] Page hidden, pausing refresh');
            // Could disable refresh when tab is hidden to save resources
        } else {
            console.log('[Auto-Refresh] Page visible, resuming refresh');
            // Resume refresh when tab becomes visible again
            checkForLiveMatches();
        }
    });

})();
