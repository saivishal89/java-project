/**
 * Student Management Portal - Frontend Application
 * Interacts with Java built-in REST API (com.sun.net.httpserver + JDBC + MySQL)
 */

// Application State
let allStudents = [];
let filteredStudents = [];
let currentView = 'table'; // 'table' | 'cards'
let studentToDelete = null;

// DOM Elements
const studentsTableBody = document.getElementById('studentsTableBody');
const cardsContainer = document.getElementById('cardsContainer');
const tableContainer = document.getElementById('tableContainer');
const emptyState = document.getElementById('emptyState');
const resultsCount = document.getElementById('resultsCount');

// Stats Elements
const statTotalStudents = document.getElementById('statTotalStudents');
const statDepartments = document.getElementById('statDepartments');
const statCourses = document.getElementById('statCourses');
const dbStatusText = document.getElementById('dbStatusText');

// Controls
const searchInput = document.getElementById('searchInput');
const clearSearchBtn = document.getElementById('clearSearchBtn');
const searchFieldSelect = document.getElementById('searchFieldSelect');
const deptFilterSelect = document.getElementById('deptFilterSelect');
const sortBySelect = document.getElementById('sortBySelect');
const tableViewBtn = document.getElementById('tableViewBtn');
const cardViewBtn = document.getElementById('cardViewBtn');
const refreshBtn = document.getElementById('refreshBtn');
const themeToggleBtn = document.getElementById('themeToggleBtn');
const themeIcon = document.getElementById('themeIcon');

// Add / Edit Modal Elements
const studentModal = document.getElementById('studentModal');
const modalTitle = document.getElementById('modalTitle');
const modalHeaderIcon = document.getElementById('modalHeaderIcon');
const studentForm = document.getElementById('studentForm');
const openAddModalBtn = document.getElementById('openAddModalBtn');
const emptyAddBtn = document.getElementById('emptyAddBtn');
const closeModalBtn = document.getElementById('closeModalBtn');
const cancelModalBtn = document.getElementById('cancelModalBtn');
const studentIdInput = document.getElementById('studentIdInput');

// Form Input Elements
const nameInput = document.getElementById('nameInput');
const emailInput = document.getElementById('emailInput');
const phoneInput = document.getElementById('phoneInput');
const ageInput = document.getElementById('ageInput');
const genderSelect = document.getElementById('genderSelect');
const departmentInput = document.getElementById('departmentInput');
const semesterInput = document.getElementById('semesterInput');
const courseInput = document.getElementById('courseInput');
const addressInput = document.getElementById('addressInput');

// View Details Modal Elements
const viewModal = document.getElementById('viewModal');
const viewContent = document.getElementById('viewContent');
const closeViewModalBtn = document.getElementById('closeViewModalBtn');
const closeViewBtn = document.getElementById('closeViewBtn');
const editFromViewBtn = document.getElementById('editFromViewBtn');
let currentlyViewingStudentId = null;

// Delete Modal Elements
const deleteModal = document.getElementById('deleteModal');
const deleteStudentName = document.getElementById('deleteStudentName');
const deleteStudentId = document.getElementById('deleteStudentId');
const cancelDeleteBtn = document.getElementById('cancelDeleteBtn');
const confirmDeleteBtn = document.getElementById('confirmDeleteBtn');

// Toast Container
const toastContainer = document.getElementById('toastContainer');

// ==========================================================================
//                           INITIALIZATION
// ==========================================================================
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    setupEventListeners();
    loadAllData();
});

function setupEventListeners() {
    // Theme toggle
    themeToggleBtn.addEventListener('click', toggleTheme);

    // Refresh
    refreshBtn.addEventListener('click', () => {
        refreshBtn.querySelector('i').classList.add('fa-spin');
        loadAllData().finally(() => {
            setTimeout(() => refreshBtn.querySelector('i').classList.remove('fa-spin'), 600);
        });
    });

    // View toggles
    tableViewBtn.addEventListener('click', () => switchView('table'));
    cardViewBtn.addEventListener('click', () => switchView('cards'));

    // Search and Filters
    let searchTimeout;
    searchInput.addEventListener('input', () => {
        clearSearchBtn.style.display = searchInput.value ? 'block' : 'none';
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(applyFilters, 250);
    });

    clearSearchBtn.addEventListener('click', () => {
        searchInput.value = '';
        clearSearchBtn.style.display = 'none';
        applyFilters();
    });

    searchFieldSelect.addEventListener('change', applyFilters);
    deptFilterSelect.addEventListener('change', () => {
        updateQuickFilterActiveState(deptFilterSelect.value);
        applyFilters();
    });
    sortBySelect.addEventListener('change', applyFilters);

    // Quick Filter Chips Click
    const quickFilterChips = document.getElementById('quickFilterChips');
    if (quickFilterChips) {
        quickFilterChips.addEventListener('click', (e) => {
            const btn = e.target.closest('.chip-btn');
            if (!btn) return;
            const dept = btn.getAttribute('data-dept');
            deptFilterSelect.value = dept;
            updateQuickFilterActiveState(dept);
            applyFilters();
        });
    }

    // Keyboard shortcut (Ctrl+K or / to search)
    document.addEventListener('keydown', (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
            e.preventDefault();
            searchInput.focus();
            searchInput.select();
        }
    });

    // Modal Events
    openAddModalBtn.addEventListener('click', () => openStudentModal());
    emptyAddBtn.addEventListener('click', () => openStudentModal());
    closeModalBtn.addEventListener('click', closeStudentModal);
    cancelModalBtn.addEventListener('click', closeStudentModal);

    // Form Submit
    studentForm.addEventListener('submit', handleFormSubmit);

    // View Modal Events
    closeViewModalBtn.addEventListener('click', closeViewModal);
    closeViewBtn.addEventListener('click', closeViewModal);
    editFromViewBtn.addEventListener('click', () => {
        const id = currentlyViewingStudentId;
        closeViewModal();
        if (id) {
            const student = allStudents.find(s => s.id === id);
            if (student) openStudentModal(student);
        }
    });

    // Delete Modal Events
    cancelDeleteBtn.addEventListener('click', closeDeleteModal);
    confirmDeleteBtn.addEventListener('click', handleDeleteConfirm);

    // Close modals on outside click
    [studentModal, viewModal, deleteModal].forEach(modal => {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.classList.remove('open');
            }
        });
    });

    // ESC key closes modals
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeStudentModal();
            closeViewModal();
            closeDeleteModal();
        }
    });
}

// ==========================================================================
//                           DATA FETCHING
// ==========================================================================

async function loadAllData() {
    await Promise.all([
        fetchStudents(),
        fetchStats(),
        checkHealth()
    ]);
}

async function checkHealth() {
    try {
        const res = await fetch('/api/health');
        if (res.ok) {
            dbStatusText.textContent = 'MySQL Connected';
            dbStatusText.parentElement.style.background = 'rgba(16, 185, 129, 0.1)';
            dbStatusText.style.color = '#10b981';
        } else {
            dbStatusText.textContent = 'DB Offline';
            dbStatusText.parentElement.style.background = 'rgba(239, 68, 68, 0.1)';
            dbStatusText.style.color = '#ef4444';
        }
    } catch {
        dbStatusText.textContent = 'Server Offline';
        dbStatusText.parentElement.style.background = 'rgba(239, 68, 68, 0.1)';
        dbStatusText.style.color = '#ef4444';
    }
}

async function fetchStats() {
    try {
        const res = await fetch('/api/stats');
        if (res.ok) {
            const stats = await res.json();
            statTotalStudents.textContent = stats.totalStudents ?? '0';
            statDepartments.textContent = stats.totalDepartments ?? '0';
            statCourses.textContent = stats.totalCourses ?? '0';

            // Populate department filter dropdown
            populateDeptFilter(stats.departments);
        }
    } catch (e) {
        console.error('Failed to load stats:', e);
    }
}

async function fetchStudents() {
    try {
        const res = await fetch('/api/students');
        if (res.ok) {
            allStudents = await res.json();
            applyFilters();
        } else {
            showToast('Failed to load students from database', 'error');
        }
    } catch (e) {
        console.error('Fetch error:', e);
        showToast('Unable to connect to local server', 'error');
    }
}

function populateDeptFilter(deptMap) {
    const currentVal = deptFilterSelect.value;
    deptFilterSelect.innerHTML = '<option value="ALL">All Departments</option>';

    if (deptMap) {
        Object.keys(deptMap).sort().forEach(dept => {
            const opt = document.createElement('option');
            opt.value = dept;
            opt.textContent = `${dept} (${deptMap[dept]})`;
            deptFilterSelect.appendChild(opt);
        });
    }

    if (currentVal) {
        deptFilterSelect.value = currentVal;
    }
}

function updateQuickFilterActiveState(selectedDept) {
    const chips = document.querySelectorAll('#quickFilterChips .chip-btn');
    chips.forEach(chip => {
        const chipDept = chip.getAttribute('data-dept');
        if (chipDept === selectedDept || (selectedDept === 'ALL' && chipDept === 'ALL')) {
            chip.classList.add('active');
        } else {
            chip.classList.remove('active');
        }
    });
}

// ==========================================================================
//                           FILTER & SORT LOGIC
// ==========================================================================

function applyFilters() {
    const query = searchInput.value.trim().toLowerCase();
    const searchField = searchFieldSelect.value;
    const selectedDept = deptFilterSelect.value;
    const sortBy = sortBySelect.value;

    filteredStudents = allStudents.filter(student => {
        // Department filter
        if (selectedDept !== 'ALL') {
            if (!student.department || student.department.toLowerCase() !== selectedDept.toLowerCase()) {
                return false;
            }
        }

        // Search keyword filter
        if (!query) return true;

        if (searchField === 'name') {
            return (student.name || '').toLowerCase().includes(query);
        } else if (searchField === 'email') {
            return (student.email || '').toLowerCase().includes(query);
        } else if (searchField === 'course') {
            return (student.course || '').toLowerCase().includes(query);
        } else if (searchField === 'department') {
            return (student.department || '').toLowerCase().includes(query);
        }
        return true;
    });

    // Sorting
    filteredStudents.sort((a, b) => {
        switch (sortBy) {
            case 'id-asc': return a.id - b.id;
            case 'id-desc': return b.id - a.id;
            case 'name-asc': return (a.name || '').localeCompare(b.name || '');
            case 'name-desc': return (b.name || '').localeCompare(a.name || '');
            case 'sem-asc': return a.semester - b.semester;
            default: return a.id - b.id;
        }
    });

    renderStudents();
}

// ==========================================================================
//                           RENDERING
// ==========================================================================

function renderStudents() {
    resultsCount.textContent = `Showing ${filteredStudents.length} of ${allStudents.length} student${allStudents.length === 1 ? '' : 's'}`;

    if (filteredStudents.length === 0) {
        tableContainer.style.display = 'none';
        cardsContainer.style.display = 'none';
        emptyState.style.display = 'block';
        return;
    }

    emptyState.style.display = 'none';

    if (currentView === 'table') {
        tableContainer.style.display = 'block';
        cardsContainer.style.display = 'none';
        renderTable();
    } else {
        tableContainer.style.display = 'none';
        cardsContainer.style.display = 'grid';
        renderCards();
    }
}

function renderTable() {
    studentsTableBody.innerHTML = filteredStudents.map(student => {
        const initials = getInitials(student.name);
        const deptClass = getDeptClass(student.department);

        return `
            <tr>
                <td><span class="student-id-badge">#${student.id}</span></td>
                <td>
                    <div class="student-identity">
                        <div class="student-avatar">${initials}</div>
                        <div class="student-name-box">
                            <span class="student-name">${escapeHtml(student.name)}</span>
                            <span class="student-subinfo">${escapeHtml(student.address || '')}</span>
                        </div>
                    </div>
                </td>
                <td>
                    <div class="contact-box">
                        <span class="contact-email"><i class="fa-solid fa-envelope"></i> ${escapeHtml(student.email)}</span>
                        <span class="contact-phone"><i class="fa-solid fa-phone"></i> ${escapeHtml(student.phone)}</span>
                    </div>
                </td>
                <td>
                    <div class="academic-box">
                        <span class="course-title">${escapeHtml(student.course)}</span>
                        <div class="tags-row">
                            <span class="dept-badge ${deptClass}">${escapeHtml(student.department)}</span>
                            <span class="sem-badge">Sem ${student.semester}</span>
                        </div>
                    </div>
                </td>
                <td>
                    <div style="font-size: 13px;">
                        <strong>${student.age} yrs</strong> <span style="color: var(--text-muted);">• ${student.gender}</span>
                    </div>
                </td>
                <td>
                    <div class="action-buttons">
                        <button class="action-btn view" onclick="openViewModal(${student.id})" title="View Details">
                            <i class="fa-solid fa-eye"></i>
                        </button>
                        <button class="action-btn edit" onclick="editStudentById(${student.id})" title="Edit Student">
                            <i class="fa-solid fa-pen"></i>
                        </button>
                        <button class="action-btn delete" onclick="confirmDelete(${student.id})" title="Delete Student">
                            <i class="fa-solid fa-trash-can"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function renderCards() {
    cardsContainer.innerHTML = filteredStudents.map(student => {
        const initials = getInitials(student.name);
        const deptClass = getDeptClass(student.department);

        return `
            <div class="student-card">
                <div>
                    <div class="card-top">
                        <div class="card-profile">
                            <div class="student-avatar">${initials}</div>
                            <div>
                                <h4 class="card-name">${escapeHtml(student.name)}</h4>
                                <span class="student-id-badge">ID: #${student.id}</span>
                            </div>
                        </div>
                        <span class="dept-badge ${deptClass}">${escapeHtml(student.department)}</span>
                    </div>

                    <div class="card-details">
                        <div class="card-item">
                            <i class="fa-solid fa-book"></i>
                            <span>${escapeHtml(student.course)} (Sem ${student.semester})</span>
                        </div>
                        <div class="card-item">
                            <i class="fa-solid fa-envelope"></i>
                            <span>${escapeHtml(student.email)}</span>
                        </div>
                        <div class="card-item">
                            <i class="fa-solid fa-phone"></i>
                            <span>${escapeHtml(student.phone)}</span>
                        </div>
                        <div class="card-item">
                            <i class="fa-solid fa-user"></i>
                            <span>${student.age} years old • ${student.gender}</span>
                        </div>
                    </div>
                </div>

                <div class="card-actions">
                    <button class="btn btn-secondary" style="padding: 6px 12px; font-size: 12px;" onclick="openViewModal(${student.id})">
                        <i class="fa-solid fa-eye"></i> View Profile
                    </button>
                    <div style="display: flex; gap: 6px;">
                        <button class="action-btn edit" onclick="editStudentById(${student.id})" title="Edit">
                            <i class="fa-solid fa-pen"></i>
                        </button>
                        <button class="action-btn delete" onclick="confirmDelete(${student.id})" title="Delete">
                            <i class="fa-solid fa-trash-can"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function switchView(view) {
    currentView = view;
    if (view === 'table') {
        tableViewBtn.classList.add('active');
        cardViewBtn.classList.remove('active');
    } else {
        cardViewBtn.classList.add('active');
        tableViewBtn.classList.remove('active');
    }
    renderStudents();
}

// ==========================================================================
//                           MODAL ACTIONS
// ==========================================================================

function openStudentModal(student = null) {
    clearFormErrors();
    studentForm.reset();

    if (student) {
        // Edit Mode
        modalTitle.textContent = 'Edit Student Record';
        modalHeaderIcon.className = 'fa-solid fa-pen-to-square';
        studentIdInput.value = student.id;
        nameInput.value = student.name || '';
        emailInput.value = student.email || '';
        phoneInput.value = student.phone || '';
        ageInput.value = student.age || '';
        genderSelect.value = student.gender || 'Male';
        departmentInput.value = student.department || '';
        semesterInput.value = student.semester || '1';
        courseInput.value = student.course || '';
        addressInput.value = student.address || '';
    } else {
        // Add Mode
        modalTitle.textContent = 'Add New Student';
        modalHeaderIcon.className = 'fa-solid fa-user-plus';
        studentIdInput.value = '';
        semesterInput.value = '1';
        ageInput.value = '20';
    }

    studentModal.classList.add('open');
}

function closeStudentModal() {
    studentModal.classList.remove('open');
}

function editStudentById(id) {
    const student = allStudents.find(s => s.id === id);
    if (student) openStudentModal(student);
}

// ==========================================================================
//                           FORM SUBMISSION (ADD / UPDATE)
// ==========================================================================

async function handleFormSubmit(e) {
    e.preventDefault();
    clearFormErrors();

    const isEdit = !!studentIdInput.value;
    const studentData = {
        name: nameInput.value.trim(),
        email: emailInput.value.trim(),
        phone: phoneInput.value.trim(),
        age: parseInt(ageInput.value, 10),
        gender: genderSelect.value,
        department: departmentInput.value.trim(),
        semester: parseInt(semesterInput.value, 10),
        course: courseInput.value.trim(),
        address: addressInput.value.trim()
    };

    // Client-side quick validation
    if (!validateClientForm(studentData)) {
        return;
    }

    const saveBtn = document.getElementById('saveStudentBtn');
    const originalText = saveBtn.innerHTML;
    saveBtn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Saving...';
    saveBtn.disabled = true;

    try {
        const url = isEdit ? `/api/students/${studentIdInput.value}` : '/api/students';
        const method = isEdit ? 'PUT' : 'POST';

        const res = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(studentData)
        });

        const data = await res.json();

        if (res.ok) {
            closeStudentModal();
            showToast(isEdit ? 'Student updated successfully!' : 'Student added successfully!', 'success');
            await loadAllData();
        } else {
            showToast(data.error || 'Operation failed', 'error');
        }
    } catch (err) {
        showToast('Network error while saving student', 'error');
    } finally {
        saveBtn.innerHTML = originalText;
        saveBtn.disabled = false;
    }
}

function validateClientForm(data) {
    let valid = true;

    if (!data.name || data.name.length < 2) {
        setFieldError('nameError', 'Name must have at least 2 characters');
        valid = false;
    }

    const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
    if (!emailRegex.test(data.email)) {
        setFieldError('emailError', 'Enter a valid email address');
        valid = false;
    }

    const phoneRegex = /^\+?[0-9]{10,15}$/;
    if (!phoneRegex.test(data.phone)) {
        setFieldError('phoneError', 'Phone must be 10-15 digits');
        valid = false;
    }

    if (isNaN(data.age) || data.age < 15 || data.age > 60) {
        setFieldError('ageError', 'Age must be between 15 and 60');
        valid = false;
    }

    if (isNaN(data.semester) || data.semester < 1 || data.semester > 8) {
        setFieldError('semError', 'Semester must be 1 to 8');
        valid = false;
    }

    return valid;
}

function setFieldError(elementId, msg) {
    const el = document.getElementById(elementId);
    if (el) el.textContent = msg;
}

function clearFormErrors() {
    ['nameError', 'emailError', 'phoneError', 'ageError', 'genderError', 'deptError', 'semError', 'courseError', 'addressError']
        .forEach(id => {
            const el = document.getElementById(id);
            if (el) el.textContent = '';
        });
}

// ==========================================================================
//                           VIEW DETAILS MODAL
// ==========================================================================

function openViewModal(id) {
    const student = allStudents.find(s => s.id === id);
    if (!student) return;

    currentlyViewingStudentId = id;
    const initials = getInitials(student.name);
    const deptClass = getDeptClass(student.department);

    viewContent.innerHTML = `
        <div class="profile-card-header">
            <div class="profile-avatar-lg">${initials}</div>
            <div>
                <h3 style="font-size: 20px; font-weight: 700;">${escapeHtml(student.name)}</h3>
                <div style="display: flex; gap: 8px; margin-top: 6px;">
                    <span class="student-id-badge">ID: #${student.id}</span>
                    <span class="dept-badge ${deptClass}">${escapeHtml(student.department)}</span>
                    <span class="sem-badge">Semester ${student.semester}</span>
                </div>
            </div>
        </div>

        <div class="profile-details-grid">
            <div class="profile-detail-item">
                <div class="profile-detail-label">Email Address</div>
                <div class="profile-detail-val">${escapeHtml(student.email)}</div>
            </div>

            <div class="profile-detail-item">
                <div class="profile-detail-label">Phone Number</div>
                <div class="profile-detail-val">${escapeHtml(student.phone)}</div>
            </div>

            <div class="profile-detail-item">
                <div class="profile-detail-label">Age & Gender</div>
                <div class="profile-detail-val">${student.age} Years • ${student.gender}</div>
            </div>

            <div class="profile-detail-item">
                <div class="profile-detail-label">Department</div>
                <div class="profile-detail-val">${escapeHtml(student.department)}</div>
            </div>

            <div class="profile-detail-item full-span">
                <div class="profile-detail-label">Enrolled Course</div>
                <div class="profile-detail-val" style="color: var(--primary);">${escapeHtml(student.course)}</div>
            </div>

            <div class="profile-detail-item full-span">
                <div class="profile-detail-label">Residential Address</div>
                <div class="profile-detail-val">${escapeHtml(student.address || 'N/A')}</div>
            </div>

            <div class="profile-detail-item full-span">
                <div class="profile-detail-label">Created At (MySQL Timestamp)</div>
                <div class="profile-detail-val" style="font-family: var(--font-mono); font-size: 13px;">
                    ${student.createdAt ? new Date(student.createdAt).toLocaleString() : 'System Default'}
                </div>
            </div>
        </div>
    `;

    viewModal.classList.add('open');
}

function closeViewModal() {
    viewModal.classList.remove('open');
    currentlyViewingStudentId = null;
}

// ==========================================================================
//                           DELETE MODAL
// ==========================================================================

function confirmDelete(id) {
    const student = allStudents.find(s => s.id === id);
    if (!student) return;

    studentToDelete = student;
    deleteStudentName.textContent = student.name;
    deleteStudentId.textContent = `#${student.id}`;
    deleteModal.classList.add('open');
}

function closeDeleteModal() {
    deleteModal.classList.remove('open');
    studentToDelete = null;
}

async function handleDeleteConfirm() {
    if (!studentToDelete) return;

    const id = studentToDelete.id;
    confirmDeleteBtn.disabled = true;
    confirmDeleteBtn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Deleting...';

    try {
        const res = await fetch(`/api/students/${id}`, { method: 'DELETE' });
        const data = await res.json();

        if (res.ok) {
            closeDeleteModal();
            showToast(`Student #${id} deleted successfully`, 'success');
            await loadAllData();
        } else {
            showToast(data.error || 'Failed to delete student', 'error');
        }
    } catch {
        showToast('Network error while deleting student', 'error');
    } finally {
        confirmDeleteBtn.disabled = false;
        confirmDeleteBtn.innerHTML = '<i class="fa-solid fa-trash-can"></i> Yes, Delete Record';
    }
}

// ==========================================================================
//                           HELPERS & TOASTS
// ==========================================================================

function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    let icon = 'fa-circle-info';
    if (type === 'success') icon = 'fa-circle-check';
    if (type === 'error') icon = 'fa-circle-exclamation';

    toast.innerHTML = `
        <i class="fa-solid ${icon}"></i>
        <span>${escapeHtml(message)}</span>
    `;

    toastContainer.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideIn 0.3s ease reverse forwards';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

function getInitials(name) {
    if (!name) return 'S';
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
        return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
}

function getDeptClass(dept) {
    if (!dept) return 'other';
    const d = dept.trim().toUpperCase();
    if (d === 'CSE') return 'cse';
    if (d === 'IT') return 'it';
    if (d === 'ECE') return 'ece';
    if (d === 'MECH' || d === 'ME') return 'mech';
    if (d === 'CIVIL') return 'civil';
    return 'other';
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function initTheme() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    if (savedTheme === 'dark') {
        document.body.classList.remove('light-theme');
        document.body.classList.add('dark-theme');
        if (themeIcon) themeIcon.className = 'fa-solid fa-sun';
    } else {
        document.body.classList.remove('dark-theme');
        document.body.classList.add('light-theme');
        if (themeIcon) themeIcon.className = 'fa-solid fa-moon';
    }
}

function toggleTheme() {
    if (document.body.classList.contains('light-theme')) {
        document.body.classList.remove('light-theme');
        document.body.classList.add('dark-theme');
        if (themeIcon) themeIcon.className = 'fa-solid fa-sun';
        localStorage.setItem('theme', 'dark');
    } else {
        document.body.classList.remove('dark-theme');
        document.body.classList.add('light-theme');
        if (themeIcon) themeIcon.className = 'fa-solid fa-moon';
        localStorage.setItem('theme', 'light');
    }
}
