package gov.samhsa.c2s.pcm.infrastructure.jdbcsupport;

import gov.samhsa.c2s.pcm.infrastructure.exception.JdbcPagingException;
import gov.samhsa.c2s.pcm.infrastructure.jdbcsupport.sql.SqlFromClause;
import gov.samhsa.c2s.pcm.infrastructure.jdbcsupport.sql.SqlGenerator;
import gov.samhsa.c2s.pcm.infrastructure.jdbcsupport.sql.SqlScriptProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class JdbcPagingRepositoryImpl implements JdbcPagingRepository {
    private static final String FROM_PATTERN = "FROM";
    private static final String SELECT_PATTERN = "SELECT";

    private final JdbcOperations jdbcOperations;
    private final SqlScriptProvider sqlScriptProvider;
    private final QueryMappingConfig queryMappingConfig;

    @Autowired
    public JdbcPagingRepositoryImpl(JdbcOperations jdbcOperations, SqlScriptProvider sqlScriptProvider, QueryMappingConfig queryMappingConfig) {
        this.jdbcOperations = jdbcOperations;
        this.sqlScriptProvider = sqlScriptProvider;
        this.queryMappingConfig = queryMappingConfig;
    }

    @Override
    public <T> Page<T> findAll(int indexOfSqls, Pageable pageable) {
        try {
            SqlGenerator sqlGenerator = getSqlGenerator(indexOfSqls);
            SqlFromClause sqlFromClause = getSqlFromClause(indexOfSqls);
            String query = sqlGenerator.selectAll(sqlFromClause, pageable);
            log.debug("Query: " + query);
            return new PageImpl<T>(jdbcOperations.query(query, queryMappingConfig.getRowMapper()), pageable, count(sqlGenerator, sqlFromClause));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new JdbcPagingException(e);
        }
    }

    @Override
    public <T> Page<T> findAllByArgs(int indexOfSqls, Pageable pageable, Object... args) {
        try {
            SqlGenerator sqlGenerator = getSqlGenerator(indexOfSqls);
            SqlFromClause sqlFromClause = getSqlFromClause(indexOfSqls);
            String query = sqlGenerator.selectByIdPageable(sqlFromClause, pageable);

            return new PageImpl<T>(jdbcOperations.query(query, queryMappingConfig.getRowMapper(), args), pageable, countByArgs(sqlGenerator, sqlFromClause, args[0]));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new JdbcPagingException(e);
        }
    }

    private long count(SqlGenerator sqlGenerator, SqlFromClause sqlFromClause) {
        return jdbcOperations.queryForObject(sqlGenerator.count(sqlFromClause), Long.class);
    }

    private long countByArgs(SqlGenerator sqlGenerator, SqlFromClause sqlFromClause, Object arg) {
        return jdbcOperations.queryForObject(sqlGenerator.countByArgs(sqlFromClause, arg), Long.class);
    }

    private SqlGenerator getSqlGenerator(int indexOfSqls) {
        String sqlScript = sqlScriptProvider.getSqlScriptByIndex(indexOfSqls);
        Pattern pattern = Pattern.compile(FROM_PATTERN);
        Matcher matcher = pattern.matcher(sqlScript);
        String selectClause = "*";
        while (matcher.find()) {
            selectClause = sqlScript.substring(SELECT_PATTERN.length(), matcher.start());
        }
        return new SqlGenerator(selectClause);
    }

    private SqlFromClause getSqlFromClause(int indexOfSqls) {
        String sqlScript = sqlScriptProvider.getSqlScriptByIndex(indexOfSqls);
        Pattern pattern = Pattern.compile(FROM_PATTERN);
        Matcher matcher = pattern.matcher(sqlScript);
        String fromClause = null;
        if (matcher.find()) {
            fromClause = sqlScript.substring(matcher.end());
        }
        return new SqlFromClause(queryMappingConfig.getTableName(), fromClause, queryMappingConfig.getIdColumn());
    }
}
