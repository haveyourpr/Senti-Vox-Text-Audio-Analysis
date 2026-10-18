local lastUpdatedKey = KEYS[1]
local tokenCountKey = KEYS[2]
local currentDate = ARGV[1]
local dailyFreeTokens = tonumber(ARGV[2])
local deductAmount = tonumber(ARGV[3])

local last_updated = redis.call('GET', lastUpdatedKey)
local current_tokens_raw = redis.call('GET', tokenCountKey)

-- Check if the keys actually exist in Redis.
-- If either is missing, it means the API key is invalid/uninitialized.
if not last_updated or not current_tokens_raw then
    return -2  -- Custom error code for "Key not found"
end

local current_tokens = tonumber(current_tokens_raw)

-- Logic for Daily Reset
if last_updated < currentDate then
    last_updated = currentDate
    current_tokens = dailyFreeTokens
    redis.call('SET', lastUpdatedKey, currentDate)
    -- We don't return here; we continue to the deduction logic below
end

-- Logic for Deduction
if current_tokens < deductAmount then
    return -1 -- Not enough tokens
else
    local remaining = current_tokens - deductAmount
    redis.call('SET', tokenCountKey, remaining)
    return remaining
end