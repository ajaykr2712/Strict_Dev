#!/bin/bash

# Run All Real-World Case Studies
# This script compiles and runs all three implementations

echo "======================================================="
echo "Real-World Backend Technologies - Case Studies Runner"
echo "======================================================="
echo ""
echo "This script will run demonstrations of:"
echo "  1. Meta's GraphQL API Gateway"
echo "  2. Amazon's Distributed Cache System"
echo "  3. Google's Rate Limiting System"
echo ""
read -p "Press Enter to continue..."
echo ""

# Set the directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Run Meta GraphQL Demo
echo ""
echo "█████████████████████████████████████████████████████"
echo "█  1. META GRAPHQL API GATEWAY                      █"
echo "█████████████████████████████████████████████████████"
echo ""
cd "$SCRIPT_DIR/Meta"
bash run.sh

echo ""
read -p "Press Enter to continue to Amazon demo..."

# Run Amazon Cache Demo
echo ""
echo "█████████████████████████████████████████████████████"
echo "█  2. AMAZON DISTRIBUTED CACHE SYSTEM               █"
echo "█████████████████████████████████████████████████████"
echo ""
cd "$SCRIPT_DIR/Amazon"
bash run.sh

echo ""
read -p "Press Enter to continue to Google demo..."

# Run Google Rate Limiter Demo
echo ""
echo "█████████████████████████████████████████████████████"
echo "█  3. GOOGLE RATE LIMITING SYSTEM                   █"
echo "█████████████████████████████████████████████████████"
echo ""
cd "$SCRIPT_DIR/Google"
bash run.sh

# Summary
echo ""
echo "======================================================="
echo "All demonstrations completed successfully!"
echo "======================================================="
echo ""
echo "Summary:"
echo "  ✓ Meta GraphQL Gateway - Efficient data fetching"
echo "  ✓ Amazon Distributed Cache - Sub-millisecond latency"
echo "  ✓ Google Rate Limiting - API protection"
echo ""
echo "For more information, see IMPLEMENTATION_GUIDE.md"
echo ""
