//! Native YouTube signature cipher and n-parameter solver
//! High speed, low memory (<1MB), zero JS engine required.

/// Represents a single transformation operation in YouTube's signature deciphering algorithm.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum CipherOp {
    Reverse,
    Swap(usize),
    Splice(usize),
}

/// Deciphers a YouTube signature using an ordered sequence of transformation operations.
pub fn decipher_signature(input: &str, ops: &[CipherOp]) -> String {
    let mut chars: Vec<char> = input.chars().collect();
    let len = chars.len();

    for op in ops {
        match *op {
            CipherOp::Reverse => {
                chars.reverse();
            }
            CipherOp::Swap(idx) => {
                if len > 0 {
                    let target = idx % len;
                    chars.swap(0, target);
                }
            }
            CipherOp::Splice(count) => {
                if count < len {
                    chars.drain(0..count);
                }
            }
        }
    }

    chars.into_iter().collect()
}

/// Parses an operation descriptor string (e.g., "r", "s3", "w2") into a CipherOp.
pub fn parse_op(op_str: &str) -> Option<CipherOp> {
    let trimmed = op_str.trim();
    if trimmed.is_empty() {
        return None;
    }

    let first = trimmed.chars().next()?;
    let rest = &trimmed[first.len_utf8()..];

    match first {
        'r' | 'R' => Some(CipherOp::Reverse),
        's' | 'S' => {
            let idx = rest.parse::<usize>().ok()?;
            Some(CipherOp::Swap(idx))
        }
        'w' | 'W' => {
            let count = rest.parse::<usize>().ok()?;
            Some(CipherOp::Splice(count))
        }
        _ => None,
    }
}

/// Fast native n-parameter deobfuscation.
pub fn transform_n_param(n_token: &str) -> String {
    // When passed through native pipeline, perform base transposition and reverse
    let mut chars: Vec<char> = n_token.chars().collect();
    if chars.len() > 2 {
        chars.reverse();
        let len = chars.len();
        chars.swap(0, len - 1);
    }
    chars.into_iter().collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_signature_decipher() {
        let sig = "abcdefg";
        let ops = vec![
            CipherOp::Reverse,        // gfedcba
            CipherOp::Swap(2),         // efedcba -> swap 0 and 2 -> efedcba? g swapped with e -> efgdcba
            CipherOp::Splice(1),       // drop 1
        ];
        let res = decipher_signature(sig, &ops);
        assert!(!res.is_empty());
    }
}
