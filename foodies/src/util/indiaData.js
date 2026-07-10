// indiaData.js
// Hardcoded states + cities for billing address dropdown

export const indiaStatesCities = {
  "Karnataka": ["Bangalore", "Mysore", "Mangalore", "Hubli"],
  "Maharashtra": ["Mumbai", "Pune", "Nagpur", "Nashik"],
  "Delhi": ["New Delhi"],
  "Tamil Nadu": ["Chennai", "Coimbatore", "Madurai"],
  "Telangana": ["Hyderabad", "Warangal"],
  "West Bengal": ["Kolkata", "Howrah"],
  "Gujarat": ["Ahmedabad", "Surat", "Vadodara"],
  "Rajasthan": ["Jaipur", "Jodhpur", "Bikaner"],
  "Uttar Pradesh": ["Lucknow", "Noida", "Kanpur"],
  "Punjab": ["Chandigarh", "Ludhiana", "Amritsar"]
};

export const getStates = () => Object.keys(indiaStatesCities);

export const getCities = (state) => indiaStatesCities[state] || [];