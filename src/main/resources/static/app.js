const API_BASE = "";

function getToken() {
    return localStorage.getItem("bank_token");
}

function getUser() {
    const raw = localStorage.getItem("bank_user");
    return raw ? JSON.parse(raw) : null;
}

function showMessage(text, isError = false) {
    const box = document.getElementById("message-box");
    box.textContent = text;
    box.className = isError ? "message error" : "message success";
    box.classList.remove("hidden");
    setTimeout(() => box.classList.add("hidden"), 5000);
}

async function apiRequest(path, method = "GET", body = null) {
    const headers = { "Content-Type": "application/json" };
    const token = getToken();
    if (token) {
        headers["Authorization"] = "Bearer " + token;
    }

    const options = { method, headers };
    if (body !== null) {
        options.body = JSON.stringify(body);
    }

    const response = await fetch(API_BASE + path, options);
    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
        throw new Error(data.message || "Request failed with status " + response.status);
    }
    return data;
}

function updateUI() {
    const user = getUser();
    const authSection = document.getElementById("auth-section");
    const customerSection = document.getElementById("customer-section");
    const adminSection = document.getElementById("admin-section");
    const userBar = document.getElementById("user-bar");

    if (!user || !getToken()) {
        authSection.classList.remove("hidden");
        customerSection.classList.add("hidden");
        adminSection.classList.add("hidden");
        userBar.classList.add("hidden");
        return;
    }

    authSection.classList.add("hidden");
    userBar.classList.remove("hidden");
    document.getElementById("welcome-text").textContent = `${user.fullName} (${user.role}) `;

    if (user.role === "ADMIN") {
        adminSection.classList.remove("hidden");
        customerSection.classList.add("hidden");
        loadAdminData();
    } else {
        customerSection.classList.remove("hidden");
        adminSection.classList.add("hidden");
        loadMyAccounts();
    }
}

async function handleLogin(event) {
    event.preventDefault();
    try {
        const email = document.getElementById("login-email").value.trim();
        const password = document.getElementById("login-password").value;
        const data = await apiRequest("/api/auth/login", "POST", { email, password });

        localStorage.setItem("bank_token", data.token);
        localStorage.setItem("bank_user", JSON.stringify(data));
        showMessage("Logged in successfully!");
        updateUI();
    } catch (err) {
        showMessage(err.message, true);
    }
}

async function handleRegister(event) {
    event.preventDefault();
    try {
        const payload = {
            fullName: document.getElementById("reg-name").value.trim(),
            email: document.getElementById("reg-email").value.trim(),
            password: document.getElementById("reg-password").value,
            phone: document.getElementById("reg-phone").value.trim(),
            address: document.getElementById("reg-address").value.trim(),
            pan: document.getElementById("reg-pan").value.trim()
        };

        const data = await apiRequest("/api/auth/register", "POST", payload);
        localStorage.setItem("bank_token", data.token);
        localStorage.setItem("bank_user", JSON.stringify(data));
        showMessage("Registration successful! Welcome to Simple Bank.");
        updateUI();
    } catch (err) {
        showMessage(err.message, true);
    }
}

function logout() {
    localStorage.removeItem("bank_token");
    localStorage.removeItem("bank_user");
    showMessage("Logged out.");
    updateUI();
}

async function loadMyAccounts() {
    try {
        const accounts = await apiRequest("/api/accounts");
        const tbody = document.getElementById("accounts-table-body");
        const txSelect = document.getElementById("tx-account-id");
        const transferSelect = document.getElementById("transfer-from-id");

        tbody.innerHTML = "";
        txSelect.innerHTML = "";
        transferSelect.innerHTML = "";

        if (accounts.length === 0) {
            tbody.innerHTML = "<tr><td colspan='6'>No accounts yet. Open one below!</td></tr>";
            return;
        }

        accounts.forEach(acc => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${acc.id}</td>
                <td><strong>${acc.accountNumber}</strong></td>
                <td>${acc.type}</td>
                <td>${Number(acc.balance).toFixed(2)}</td>
                <td>${acc.status}</td>
                <td><button class="btn btn-secondary btn-small" onclick="loadStatement(${acc.id}, '${acc.accountNumber}')">View Statement</button></td>
            `;
            tbody.appendChild(row);

            const optionHtml = `<option value="${acc.id}">${acc.accountNumber} (${acc.type} - ${Number(acc.balance).toFixed(2)})</option>`;
            txSelect.innerHTML += optionHtml;
            transferSelect.innerHTML += optionHtml;
        });
    } catch (err) {
        showMessage(err.message, true);
    }
}

async function handleCreateAccount(event) {
    event.preventDefault();
    try {
        const type = document.getElementById("new-acc-type").value;
        const initialDeposit = Number(document.getElementById("new-acc-deposit").value || 0);
        const created = await apiRequest("/api/accounts", "POST", { type, initialDeposit });
        showMessage(`Account ${created.accountNumber} opened!`);
        loadMyAccounts();
    } catch (err) {
        showMessage(err.message, true);
    }
}

async function handleDepositOrWithdraw(event) {
    event.preventDefault();
    try {
        const accountId = document.getElementById("tx-account-id").value;
        const action = document.getElementById("tx-action").value;
        const amount = Number(document.getElementById("tx-amount").value);
        const description = document.getElementById("tx-note").value.trim();

        const tx = await apiRequest(`/api/accounts/${accountId}/${action}`, "POST", { amount, description });
        showMessage(`${tx.type} of ${Number(tx.amount).toFixed(2)} successful!`);
        document.getElementById("tx-amount").value = "";
        document.getElementById("tx-note").value = "";
        loadMyAccounts();
        loadStatement(accountId, tx.accountNumber);
    } catch (err) {
        showMessage(err.message, true);
    }
}

async function handleTransfer(event) {
    event.preventDefault();
    try {
        const payload = {
            fromAccountId: Number(document.getElementById("transfer-from-id").value),
            toAccountNumber: document.getElementById("transfer-to-number").value.trim(),
            amount: Number(document.getElementById("transfer-amount").value),
            description: document.getElementById("transfer-note").value.trim()
        };

        const tx = await apiRequest("/api/transfers", "POST", payload);
        showMessage(`Transferred ${Number(tx.amount).toFixed(2)} successfully!`);
        document.getElementById("transfer-amount").value = "";
        document.getElementById("transfer-note").value = "";
        loadMyAccounts();
        loadStatement(payload.fromAccountId, tx.accountNumber);
    } catch (err) {
        showMessage(err.message, true);
    }
}

async function loadStatement(accountId, accountNumber) {
    try {
        const list = await apiRequest(`/api/accounts/${accountId}/transactions`);
        document.getElementById("statement-title").textContent = `Statement for ${accountNumber}`;
        document.getElementById("statement-hint").classList.add("hidden");

        const tbody = document.getElementById("statement-table-body");
        tbody.innerHTML = "";

        if (list.length === 0) {
            tbody.innerHTML = "<tr><td colspan='5'>No transactions found.</td></tr>";
            return;
        }

        list.forEach(tx => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${tx.id}</td>
                <td>${tx.timestamp.replace("T", " ").substring(0, 19)}</td>
                <td>${tx.type}</td>
                <td>${Number(tx.amount).toFixed(2)}</td>
                <td>${tx.description || "-"}</td>
            `;
            tbody.appendChild(row);
        });
    } catch (err) {
        showMessage(err.message, true);
    }
}

async function loadAdminData() {
    try {
        const [customers, accounts] = await Promise.all([
            apiRequest("/api/admin/customers"),
            apiRequest("/api/admin/accounts")
        ]);

        const custBody = document.getElementById("admin-customers-body");
        custBody.innerHTML = "";
        customers.forEach(c => {
            const accNums = c.accounts.map(a => `${a.accountNumber} (${a.status})`).join(", ") || "None";
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${c.id}</td>
                <td>${c.fullName}</td>
                <td>${c.email}</td>
                <td>${c.phone}</td>
                <td>${c.address}</td>
                <td>${accNums}</td>
            `;
            custBody.appendChild(row);
        });

        const accBody = document.getElementById("admin-accounts-body");
        accBody.innerHTML = "";
        accounts.forEach(a => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${a.id}</td>
                <td><strong>${a.accountNumber}</strong></td>
                <td>${a.customerName}</td>
                <td>${a.type}</td>
                <td>${Number(a.balance).toFixed(2)}</td>
                <td>${a.status}</td>
                <td>
                    <button class="btn btn-success btn-small" onclick="changeStatus(${a.id}, 'ACTIVE')">Activate</button>
                    <button class="btn btn-warn btn-small" onclick="changeStatus(${a.id}, 'FROZEN')">Freeze</button>
                    <button class="btn btn-danger btn-small" onclick="changeStatus(${a.id}, 'CLOSED')">Close</button>
                </td>
            `;
            accBody.appendChild(row);
        });

        loadFilteredTransactions();
    } catch (err) {
        showMessage(err.message, true);
    }
}

async function changeStatus(accountId, status) {
    try {
        await apiRequest(`/api/admin/accounts/${accountId}/status`, "PUT", { status });
        showMessage(`Account #${accountId} updated to ${status}`);
        loadAdminData();
    } catch (err) {
        showMessage(err.message, true);
    }
}

function handleFilterTransactions(event) {
    event.preventDefault();
    loadFilteredTransactions();
}

async function loadFilteredTransactions() {
    try {
        const type = document.getElementById("filter-type").value;
        const date = document.getElementById("filter-date").value;
        const params = new URLSearchParams();
        if (type) params.append("type", type);
        if (date) params.append("date", date);

        const query = params.toString() ? "?" + params.toString() : "";
        const list = await apiRequest("/api/admin/transactions" + query);

        const tbody = document.getElementById("admin-transactions-body");
        tbody.innerHTML = "";

        if (list.length === 0) {
            tbody.innerHTML = "<tr><td colspan='6'>No matching transactions.</td></tr>";
            return;
        }

        list.forEach(tx => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td>${tx.id}</td>
                <td>${tx.accountNumber}</td>
                <td>${tx.type}</td>
                <td>${Number(tx.amount).toFixed(2)}</td>
                <td>${tx.timestamp.replace("T", " ").substring(0, 19)}</td>
                <td>${tx.description || "-"}</td>
            `;
            tbody.appendChild(row);
        });
    } catch (err) {
        showMessage(err.message, true);
    }
}

updateUI();
