 
// ════════════════════════════════════════════════════════════════════
//  INTERFACE — Manageable  (CRUD contract)
// ════════════════════════════════════════════════════════════════════
interface Manageable
{
    void create(Resource r);
    Resource read(int id);
    void update(int id, Resource r);
    void delete(int id);
}
