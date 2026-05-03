import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import client from '../api/client';

export default function GroupPage() {
  const { groupId } = useParams();
  const navigate = useNavigate();
  const [group, setGroup] = useState(null);
  const [expenses, setExpenses] = useState([]);
  const [balances, setBalances] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showExpenseModal, setShowExpenseModal] = useState(false);
  const [activeTab, setActiveTab] = useState('expenses');
  const [parseInput, setParseInput] = useState('');
  const [parsing, setParsing] = useState(false);
  const [parseResult, setParseResult] = useState(null);
  const [parseError, setParseError] = useState('');
  const [form, setForm] = useState({
    description: '', amount: '', currency: 'USD',
    splitType: 'EQUAL', paidById: ''
  });
  const [submitting, setSubmitting] = useState(false);
  const [showMemberModal, setShowMemberModal] = useState(false);
  const [memberEmail, setMemberEmail] = useState('');
  const [addingMember, setAddingMember] = useState(false);
  const [memberError, setMemberError] = useState('');
  const [activity, setActivity] = useState([]);

  useEffect(() => {
    fetchAll();
  }, [groupId]);

  const fetchAll = async () => {
    try {
      const [groupsRes, expensesRes, balancesRes, activityRes] = await Promise.all([
        client.get('/api/groups'),
        client.get(`/api/expenses/group/${groupId}`),
        client.get(`/api/settlements/group/${groupId}/balances`),
        client.get(`/api/activity/group/${groupId}`)
      ]);
      const g = groupsRes.data.find(g => g.id === groupId);
      setGroup(g);
      setExpenses(expensesRes.data);
      setBalances(balancesRes.data);
      setActivity(activityRes.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

const addExpense = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await client.post('/api/expenses', {
        groupId,
        description: form.description,
        amount: parseFloat(form.amount),
        currency: form.currency,
        splitType: form.splitType,
        splits: [],
        paidById: form.paidById || null
      });
      setForm({ description: '', amount: '', currency: 'USD', splitType: 'EQUAL' });
      await fetchAll();        // fetch first
      setShowExpenseModal(false); // then close modal
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="min-h-screen bg-gray-50 flex items-center justify-center"><p className="text-gray-400">Loading...</p></div>;

const parseExpense = async (e) => {
  e.preventDefault();
  setParsing(true);
  setParseResult(null);
  setParseError('');
  try {
    const res = await client.post('/api/expenses/parse', {
      input: parseInput,
      groupId
    });
    setParseResult(res.data);
    if (res.data.autoCreated) {
      setParseInput('');
      fetchAll();
    }
  } catch (err) {
    setParseError('Failed to parse expense');
  } finally {
    setParsing(false);
  }
};

const addMember = async (e) => {
  e.preventDefault();
  e.stopPropagation();
  if (addingMember) return;
  setAddingMember(true);
  setMemberError('');
  try {
    const userRes = await client.get(`/api/users/search?email=${encodeURIComponent(memberEmail)}`);
    if (!userRes.data || !userRes.data.id) {
      setMemberError('User not found');
      return;
    }
    await client.post(`/api/groups/${groupId}/members`, { userId: userRes.data.id });
    setMemberEmail('');
    setShowMemberModal(false);
    fetchAll();
  } catch (err) {
    console.error(err.response?.status, err.response?.data);
    setMemberError('User not found or already a member');
  } finally {
    setAddingMember(false);
  }
};
  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white border-b border-gray-200 px-6 py-4 flex items-center gap-4">
        <button onClick={() => navigate('/')} className="text-gray-400 hover:text-gray-600 text-sm">← Back</button>
        <h1 className="text-xl font-bold text-gray-900 ml-2">{group?.name}</h1>
        <div className="ml-auto flex items-center gap-4">
          <span className="text-sm text-gray-400">{group?.members?.length} member{group?.members?.length !== 1 ? 's' : ''}</span>
        </div>
        <button
          onClick={() => setShowMemberModal(true)}
          className="text-sm text-blue-600 hover:text-blue-700 font-medium"
        >
          + Add Member
        </button>
      </nav>

      <div className="max-w-2xl mx-auto px-6 py-8">
        <div className="flex gap-6 mb-6 border-b border-gray-200">
          {['expenses', 'balances', 'ai parse', 'activity'].map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`pb-3 px-1 text-sm font-medium capitalize border-b-2 transition-colors ${
                activeTab === tab ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        {activeTab === 'expenses' && (
          <>
            <div className="flex justify-between items-center mb-4">
              <p className="text-sm text-gray-500">{expenses.length} expense{expenses.length !== 1 ? 's' : ''}</p>
              <button
                onClick={() => setShowExpenseModal(true)}
                className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700"
              >
                + Add Expense
              </button>
            </div>

            {expenses.length === 0 ? (
              <div className="text-center py-16">
                <p className="text-gray-400 text-sm">No expenses yet</p>
              </div>
            ) : (
              <div className="space-y-3">
                {expenses.map(expense => (
                  <div key={expense.id} className="bg-white rounded-xl p-4 border border-gray-200">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="font-medium text-gray-900">{expense.description}</p>
                        <p className="text-sm text-gray-400 mt-0.5">
                          Paid by {expense.paidBy?.displayName} · {expense.splitType}
                        </p>
                      </div>
                      <p className="font-semibold text-gray-900">${expense.amount}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </>
        )}

        {activeTab === 'balances' && (
          <div>
            {balances.length === 0 ? (
              <div className="text-center py-16">
                <p className="text-gray-400 text-sm">All settled up!</p>
              </div>
            ) : (
              <div className="space-y-3">
                {balances.map((b, i) => (
                  <div key={i} className="bg-white rounded-xl p-4 border border-gray-200">
                    <p className="text-sm text-gray-700">
                      <span className="font-medium">{b.from}</span>
                      <span className="text-gray-400"> owes </span>
                      <span className="font-medium">{b.to}</span>
                      <span className="float-right font-semibold text-red-500">${b.amount}</span>
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

    {activeTab === 'ai parse' && (
      <div className="space-y-4">
        <p className="text-sm text-gray-500">Type a natural language expense and let AI parse it.</p>
        <form onSubmit={parseExpense} className="space-y-3">
          <input
            type="text"
            value={parseInput}
            onChange={e => setParseInput(e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder='e.g. "Split $90 dinner with Bob and Carol equally"'
            required
          />
          <button
            type="submit"
            disabled={parsing}
            className="w-full bg-blue-600 text-white py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
          >
            {parsing ? 'Parsing...' : 'Parse with AI ✨'}
          </button>
        </form>

        {parseError && (
          <div className="bg-red-50 text-red-600 px-4 py-3 rounded-lg text-sm">{parseError}</div>
        )}

        {parseResult && (
          <div className="bg-white rounded-xl p-4 border border-gray-200 space-y-2">
            {parseResult.autoCreated ? (
              <div className="flex items-center gap-2 text-green-600 text-sm font-medium">
                <span>✓</span>
                <span>Expense created automatically!</span>
              </div>
            ) : (
              <div className="text-sm text-yellow-600 font-medium">⚠ Review before confirming</div>
            )}
            {parseResult.parsed && (
              <div className="text-sm text-gray-600 space-y-1 mt-2">
                {parseResult.parsed.description && <p><span className="text-gray-400">Description:</span> {parseResult.parsed.description}</p>}
                {parseResult.parsed.amount && <p><span className="text-gray-400">Amount:</span> ${parseResult.parsed.amount}</p>}
                {parseResult.parsed.splitType && <p><span className="text-gray-400">Split:</span> {parseResult.parsed.splitType}</p>}
                {parseResult.parsed.confidence && <p><span className="text-gray-400">Confidence:</span> {parseResult.parsed.confidence}</p>}
                {parseResult.parsed.clarificationNeeded && (
                  <p className="text-yellow-600"><span className="text-gray-400">Note:</span> {parseResult.parsed.clarificationNeeded}</p>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    )}

    {activeTab === 'activity' && (
      <div className="space-y-3 mt-2">
        {activity.length === 0 ? (
          <div className="text-center py-16">
            <p className="text-gray-400 text-sm">No activity yet.</p>
          </div>
        ) : (
          activity.map(event => {
            const meta = JSON.parse(event.metadata || '{}');
            return (
              <div key={event.id} className="bg-white rounded-xl p-4 border border-gray-200">
                <p className="text-sm text-gray-800">
                  <span className="font-medium">{event.actorName}</span>{' '}
                  {event.eventType === 'EXPENSE_ADDED' && (
                    <span>added <span className="font-medium">{meta.expenseDescription}</span> — ${meta.amount}</span>
                  )}
                  {event.eventType === 'SETTLEMENT_RECORDED' && (
                    <span>settled <span className="font-medium">${meta.amount}</span> with {meta.payeeName}</span>
                  )}
                  {event.eventType === 'MEMBER_JOINED' && <span>joined the group</span>}
                </p>
                <p className="text-xs text-gray-400 mt-1">
                  {new Date(event.createdAt).toLocaleString()}
                </p>
              </div>
            );
          })
        )}
      </div>
    )}

      </div>

      {showExpenseModal && (
        <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl p-6 w-full max-w-sm shadow-xl">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Add Expense</h3>
            <form onSubmit={addExpense} className="space-y-4">
              <input
                type="text"
                value={form.description}
                onChange={e => setForm({...form, description: e.target.value})}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Description"
                required
                autoFocus
              />
              <input
                type="number"
                value={form.amount}
                onChange={e => setForm({...form, amount: e.target.value})}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Amount"
                min="0.01"
                step="0.01"
                required
              />
<select
  value={form.paidById}
  onChange={e => setForm({...form, paidById: e.target.value})}
  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
>
  <option value="">Me (default)</option>
  {group?.members?.map(m => (
    <option key={m.user.id} value={m.user.id}>
      {m.user.displayName}
    </option>
  ))}
</select>
<select
  value={form.splitType}
  onChange={e => setForm({...form, splitType: e.target.value})}
  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
>
  <option value="EQUAL">Equal split</option>
{/*   <option value="EXACT">Exact amounts</option> */}
</select>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setShowExpenseModal(false)}
                  className="flex-1 border border-gray-300 text-gray-700 py-2 rounded-lg text-sm font-medium hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="flex-1 bg-blue-600 text-white py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
                >
                  {submitting ? 'Adding...' : 'Add'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

  {showMemberModal && (
    <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50">
      <div className="bg-white rounded-2xl p-6 w-full max-w-sm shadow-xl">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Add Member</h3>
        <form onSubmit={addMember} className="space-y-4">
          <input
            type="email"
            value={memberEmail}
            onChange={e => setMemberEmail(e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="member@example.com"
            required
            autoFocus
          />
          {memberError && (
            <p className="text-red-500 text-sm">{memberError}</p>
          )}
          <div className="flex gap-3">
            <button
              type="button"
              onClick={() => { setShowMemberModal(false); setMemberError(''); }}
              className="flex-1 border border-gray-300 text-gray-700 py-2 rounded-lg text-sm font-medium hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={addingMember}
              className="flex-1 bg-blue-600 text-white py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
            >
              {addingMember ? 'Adding...' : 'Add'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )}
    </div>
  );
}
