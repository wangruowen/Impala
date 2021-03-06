// Copyright 2014 Cloudera Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.cloudera.impala.analysis;

import com.cloudera.impala.catalog.Role;
import com.cloudera.impala.common.AnalysisException;
import com.cloudera.impala.thrift.TShowGrantRoleParams;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Represents a "SHOW GRANT ROLE <role> [ON <privilegeSpec>]" statement.
 */
public class ShowGrantRoleStmt extends AuthorizationStmt {
  private final PrivilegeSpec privilegeSpec_;
  private final String roleName_;

  // Set/modified during analysis
  private Role role_;

  public ShowGrantRoleStmt(String roleName, PrivilegeSpec privilegeSpec) {
    Preconditions.checkNotNull(roleName);
    roleName_ = roleName;
    privilegeSpec_ = privilegeSpec;
  }

  public TShowGrantRoleParams toThrift() {
    TShowGrantRoleParams params = new TShowGrantRoleParams();
    params.setRole_name(roleName_);
    params.setRequesting_user(requestingUser_.getShortName());
    if (privilegeSpec_ != null) {
      params.setPrivilege(privilegeSpec_.toThrift());
      params.getPrivilege().setRole_id(role_.getId());
    }
    return params;
  }

  @Override
  public String toSql() {
    StringBuilder sb = new StringBuilder("SHOW GRANT ROLE ");
    sb.append(roleName_);
    if (privilegeSpec_ != null) sb.append(" " + privilegeSpec_.toSql());
    return sb.toString();
  }

  @Override
  public void analyze(Analyzer analyzer) throws AnalysisException {
    super.analyze(analyzer);
    if (Strings.isNullOrEmpty(roleName_)) {
      throw new AnalysisException("Role name in SHOW GRANT ROLE cannot be " +
          "empty.");
    }
    role_ = analyzer.getCatalog().getAuthPolicy().getRole(roleName_);
    if (role_ == null) {
      throw new AnalysisException(String.format("Role '%s' does not exist.", roleName_));
    }
    if (privilegeSpec_ != null) privilegeSpec_.analyze(analyzer);
  }

  public Role getRole() { return role_; }
}