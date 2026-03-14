INSERT INTO admins (username, password, name, election_status, winner_name)
VALUES ('admin', 'admin123', 'System Administrator', 'OPEN', NULL)
ON DUPLICATE KEY UPDATE username = username;

INSERT INTO candidates (name, party, symbol, manifesto, vote_count)
SELECT 'Aarav Mehta', 'Future Civic Party', 'AM', 'Smart governance and transparent public services', 0
WHERE NOT EXISTS (SELECT 1 FROM candidates WHERE name = 'Aarav Mehta');

INSERT INTO candidates (name, party, symbol, manifesto, vote_count)
SELECT 'Diya Sharma', 'Green Progress Alliance', 'DS', 'Sustainable cities and youth participation', 0
WHERE NOT EXISTS (SELECT 1 FROM candidates WHERE name = 'Diya Sharma');

INSERT INTO candidates (name, party, symbol, manifesto, vote_count)
SELECT 'Kabir Rao', 'Digital Reform Front', 'KR', 'Open data and digital-first citizen support', 0
WHERE NOT EXISTS (SELECT 1 FROM candidates WHERE name = 'Kabir Rao');
