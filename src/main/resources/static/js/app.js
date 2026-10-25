/**
 * Système de Livraison — app shell behaviours
 */
(function () {
    'use strict';

    var THEME_KEY = 'livraison-theme';
    var SIDEBAR_KEY = 'livraison-sidebar-collapsed';

    function ready(fn) {
        if (document.readyState !== 'loading') {
            fn();
        } else {
            document.addEventListener('DOMContentLoaded', fn);
        }
    }

    function getTheme() {
        try {
            return localStorage.getItem(THEME_KEY) ||
                document.documentElement.getAttribute('data-bs-theme') ||
                'light';
        } catch (e) {
            return 'light';
        }
    }

    function setTheme(theme) {
        var next = theme === 'dark' ? 'dark' : 'light';
        document.documentElement.setAttribute('data-bs-theme', next);
        try {
            localStorage.setItem(THEME_KEY, next);
        } catch (e) { /* ignore */ }
        document.querySelectorAll('[data-theme-toggle]').forEach(function (btn) {
            var icon = btn.querySelector('i');
            if (icon) {
                icon.className = next === 'dark' ? 'bi bi-sun' : 'bi bi-moon-stars';
            }
            btn.setAttribute('aria-label', next === 'dark' ? 'Passer en mode clair' : 'Passer en mode sombre');
            btn.setAttribute('title', next === 'dark' ? 'Mode clair' : 'Mode sombre');
        });
    }

    function toggleTheme() {
        setTheme(getTheme() === 'dark' ? 'light' : 'dark');
    }

    function isMobile() {
        return window.matchMedia('(max-width: 991.98px)').matches;
    }

    function setSidebarCollapsed(collapsed) {
        document.body.classList.toggle('sidebar-collapsed', !!collapsed);
        try {
            localStorage.setItem(SIDEBAR_KEY, collapsed ? '1' : '0');
        } catch (e) { /* ignore */ }
        document.querySelectorAll('[data-sidebar-toggle]').forEach(function (btn) {
            btn.setAttribute('aria-expanded', collapsed ? 'false' : 'true');
        });
    }

    function openMobileSidebar() {
        document.body.classList.add('sidebar-open');
        document.body.classList.remove('sidebar-collapsed');
    }

    function closeMobileSidebar() {
        document.body.classList.remove('sidebar-open');
    }

    function toggleSidebar() {
        if (isMobile()) {
            if (document.body.classList.contains('sidebar-open')) {
                closeMobileSidebar();
            } else {
                openMobileSidebar();
            }
            return;
        }
        setSidebarCollapsed(!document.body.classList.contains('sidebar-collapsed'));
    }

    function initialsFromName(name) {
        if (!name) {
            return '?';
        }
        var parts = String(name).trim().split(/\s+/).filter(Boolean);
        if (parts.length === 0) {
            return '?';
        }
        if (parts.length === 1) {
            return parts[0].substring(0, 2).toUpperCase();
        }
        return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    function fillAvatars() {
        document.querySelectorAll('[data-avatar-name]').forEach(function (el) {
            if (el.textContent && el.textContent.trim()) {
                return;
            }
            el.textContent = initialsFromName(el.getAttribute('data-avatar-name'));
        });
    }

    function initTooltips() {
        if (!window.bootstrap || !bootstrap.Tooltip) {
            return;
        }
        document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (el) {
            bootstrap.Tooltip.getOrCreateInstance(el);
        });
    }

    function initAriaLabels() {
        document.querySelectorAll('button[title]:not([aria-label]), a[title]:not([aria-label])').forEach(function (el) {
            var label = el.getAttribute('title');
            if (label && label.trim()) {
                el.setAttribute('aria-label', label.trim());
            }
        });
        document.querySelectorAll('input[type="search"][placeholder]:not([aria-label])').forEach(function (el) {
            el.setAttribute('aria-label', el.getAttribute('placeholder'));
        });
    }

    function initSubmitFeedback() {
        document.querySelectorAll('form').forEach(function (form) {
            if (form.hasAttribute('data-no-submit-feedback')) {
                return;
            }
            form.addEventListener('submit', function (ev) {
                if (form.dataset.appSubmitting === '1') {
                    return;
                }
                if (typeof form.checkValidity === 'function' && !form.checkValidity()) {
                    return;
                }
                form.dataset.appSubmitting = '1';
                var btn = ev.submitter ||
                    form.querySelector('button[type="submit"], input[type="submit"]');
                if (btn) {
                    btn.classList.add('is-loading');
                    btn.setAttribute('aria-busy', 'true');
                    btn.disabled = true;
                }
                window.setTimeout(function () {
                    form.dataset.appSubmitting = '0';
                    if (btn) {
                        btn.classList.remove('is-loading');
                        btn.removeAttribute('aria-busy');
                        btn.disabled = false;
                    }
                }, 12000);
            });
        });
    }

    function initToasts() {
        if (!window.bootstrap || !bootstrap.Toast) {
            return;
        }
        document.querySelectorAll('.toast[data-app-autotoast]').forEach(function (el) {
            var toast = bootstrap.Toast.getOrCreateInstance(el, { delay: 4500 });
            toast.show();
        });

        // Convert dismissible alerts into toasts when marked
        document.querySelectorAll('.alert[data-app-toast]').forEach(function (alertEl) {
            var container = document.querySelector('.toast-container.app-toasts');
            if (!container) {
                container = document.createElement('div');
                container.className = 'toast-container app-toasts position-fixed top-0 end-0 p-3';
                container.setAttribute('role', 'region');
                container.setAttribute('aria-live', 'polite');
                container.setAttribute('aria-label', 'Notifications');
                document.body.appendChild(container);
            }
            var toast = document.createElement('div');
            toast.className = 'toast app-toast show';
            toast.setAttribute('role', 'alert');
            toast.setAttribute('data-app-autotoast', '');
            toast.innerHTML =
                '<div class="d-flex align-items-center">' +
                '<div class="toast-body">' + (alertEl.textContent || '').trim() + '</div>' +
                '<button type="button" class="btn-close me-2 m-auto" data-bs-dismiss="toast"></button>' +
                '</div>';
            container.appendChild(toast);
            bootstrap.Toast.getOrCreateInstance(toast, { delay: 4500 }).show();
            alertEl.classList.add('d-none');
        });
    }

    function initTableFilter() {
        document.querySelectorAll('[data-table-filter]').forEach(function (input) {
            var targetSel = input.getAttribute('data-table-filter');
            var table = document.querySelector(targetSel);
            if (!table) {
                return;
            }
            var rows = table.querySelectorAll('tbody tr[data-filter-row], tbody tr:not(.table-light):not([data-no-filter])');
            input.addEventListener('input', function () {
                var q = (input.value || '').trim().toLowerCase();
                rows.forEach(function (row) {
                    if (row.querySelector('td[colspan]')) {
                        return;
                    }
                    var text = row.textContent.toLowerCase();
                    row.style.display = !q || text.indexOf(q) !== -1 ? '' : 'none';
                });
            });
        });

        // Wire topbar search to first filterable table if present
        var topSearch = document.getElementById('appTopSearch');
        var firstTable = document.querySelector('table.app-table, table.table');
        if (topSearch && firstTable && !topSearch.getAttribute('data-table-filter')) {
            topSearch.setAttribute('data-table-filter', 'table.app-table, table.table');
            topSearch.dispatchEvent(new Event('input'));
            topSearch.addEventListener('input', function () {
                var q = (topSearch.value || '').trim().toLowerCase();
                var rows = firstTable.querySelectorAll('tbody > tr');
                rows.forEach(function (row) {
                    if (row.querySelector('td[colspan]') && !row.classList.contains('table-light')) {
                        row.style.display = !q ? '' : (row.textContent.toLowerCase().indexOf(q) !== -1 ? '' : 'none');
                        return;
                    }
                    if (row.classList.contains('table-light')) {
                        return;
                    }
                    row.style.display = !q || row.textContent.toLowerCase().indexOf(q) !== -1 ? '' : 'none';
                });
            });
        }
    }

    function initDeleteConfirms() {
        document.querySelectorAll('a[data-confirm], button[data-confirm]').forEach(function (el) {
            el.addEventListener('click', function (ev) {
                var msg = el.getAttribute('data-confirm') || 'Confirmer cette action ?';
                if (!window.confirm(msg)) {
                    ev.preventDefault();
                    ev.stopPropagation();
                }
            });
        });
    }

    function initThemeButtons() {
        setTheme(getTheme());
        document.querySelectorAll('[data-theme-toggle]').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                toggleTheme();
            });
        });
    }

    function initSidebar() {
        var collapsed = false;
        try {
            collapsed = localStorage.getItem(SIDEBAR_KEY) === '1';
        } catch (e) { /* ignore */ }

        if (!isMobile() && collapsed) {
            setSidebarCollapsed(true);
        }

        document.querySelectorAll('[data-sidebar-toggle]').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                toggleSidebar();
            });
        });

        var backdrop = document.querySelector('[data-sidebar-backdrop]');
        if (backdrop) {
            backdrop.addEventListener('click', closeMobileSidebar);
        }

        document.querySelectorAll('.app-sidebar .app-nav-link').forEach(function (link) {
            link.addEventListener('click', function () {
                if (isMobile()) {
                    closeMobileSidebar();
                }
            });
        });

        window.addEventListener('resize', function () {
            if (!isMobile()) {
                closeMobileSidebar();
            }
        });
    }

    ready(function () {
        initThemeButtons();
        initSidebar();
        fillAvatars();
        initAriaLabels();
        initTooltips();
        initToasts();
        initTableFilter();
        initDeleteConfirms();
        initSubmitFeedback();
    });

    window.LivraisonApp = {
        setTheme: setTheme,
        toggleTheme: toggleTheme,
        toggleSidebar: toggleSidebar,
        initialsFromName: initialsFromName
    };
})();
